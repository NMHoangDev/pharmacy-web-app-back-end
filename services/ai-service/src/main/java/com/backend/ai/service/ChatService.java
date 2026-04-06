package com.backend.ai.service;

import com.backend.ai.api.dto.ChatRequest;
import com.backend.ai.api.dto.ChatResponse;
import com.backend.ai.api.dto.ChatSourceDto;
import com.backend.ai.api.dto.ConversationResponse;
import com.backend.ai.api.dto.ConversationTurnDto;
import com.backend.ai.client.LlmClient;
import com.backend.ai.model.ChatConversationEntity;
import com.backend.ai.model.ChatMessageEntity;
import com.backend.ai.repo.ChatConversationRepository;
import com.backend.ai.repo.ChatMessageRepository;
import com.backend.ai.service.dto.RetrievalResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final TypeReference<List<ChatSourceDto>> SOURCE_LIST_TYPE = new TypeReference<>() {};

    private static final String DISCLAIMER =
            "Thông tin từ AI chỉ nhằm hỗ trợ tham khảo, không thay thế tư vấn trực tiếp từ bác sĩ hoặc dược sĩ.";
    private static final String EMERGENCY_REPLY =
            "Câu hỏi này có dấu hiệu cần được đánh giá khẩn cấp. Nếu đang khó thở, đau ngực, ngất, co giật, sốt cao không hạ, chảy máu nhiều, mơ màng hoặc nghi quá liều, hãy đến cơ sở y tế gần nhất hoặc gọi cấp cứu ngay. Mình không nên trì hoãn bằng tư vấn online trong tình huống này.";

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final RetrievalService retrievalService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ChatService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            RetrievalService retrievalService,
            LlmClient llmClient,
            ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "llmConfigured", llmClient.isReady(),
                "model", llmClient.model());
    }

    public ChatResponse chat(ChatRequest request, Jwt jwt) {
        String userQuestion = request.messages().get(request.messages().size() - 1).content().trim();
        String retrievalQuery = buildRetrievalQuery(request);
        UUID userId = extractUserId(jwt);

        ChatConversationEntity conversation = resolveConversation(request.conversationId(), userId, userQuestion);
        ContextualProductReference productReference = resolveContextualProductReference(conversation.getId(), userQuestion);
        UUID effectiveProductId = request.productId() != null ? request.productId() : productReference.productId();
        boolean productIntent = isProductIntent(userQuestion, request.productId(), productReference);

        log.info(
                "chat request received: conversationId={}, userId={}, messageCount={}, question={}, retrievalQuery={}, productIntent={}, contextualProductId={}",
                request.conversationId(),
                userId,
                request.messages().size(),
                userQuestion,
                retrievalQuery,
                productIntent,
                effectiveProductId);

        RetrievalResult retrieval = productIntent
                ? retrievalService.retrieve(retrievalQuery, effectiveProductId, request.branchId())
                : new RetrievalResult(retrievalQuery, false, false, false, List.of(), List.of());

        List<ChatSourceDto> sources = productIntent ? retrieval.toSources() : List.of();
        String reply = generateReply(request, retrieval, productIntent, productReference);
        boolean needsHumanSupport = needsHumanSupport(userQuestion, retrieval);

        saveTurn(conversation, "user", userQuestion, null);
        saveTurn(conversation, "assistant", reply, sources);

        log.info(
                "chat response ready: conversationId={}, needsHumanSupport={}, sourceCount={}",
                conversation.getId(),
                needsHumanSupport,
                sources.size());

        return new ChatResponse(conversation.getId(), reply, needsHumanSupport, DISCLAIMER, sources);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, Jwt jwt) {
        UUID userId = extractUserId(jwt);
        ChatConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        validateConversationOwner(conversation, userId);

        List<ConversationTurnDto> turns = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> new ConversationTurnDto(
                        message.getId(),
                        message.getRoleName(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();

        return new ConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                turns);
    }

    private ChatConversationEntity resolveConversation(UUID conversationId, UUID userId, String question) {
        if (conversationId != null) {
            ChatConversationEntity conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
            validateConversationOwner(conversation, userId);
            conversation.setUpdatedAt(Instant.now());
            return conversationRepository.save(conversation);
        }

        ChatConversationEntity conversation = new ChatConversationEntity();
        conversation.setId(UUID.randomUUID());
        conversation.setUserId(userId);
        conversation.setTitle(trimTitle(question));
        return conversationRepository.save(conversation);
    }

    private void validateConversationOwner(ChatConversationEntity conversation, UUID userId) {
        if (conversation.getUserId() != null && userId != null && !conversation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conversation does not belong to current user");
        }
        if (conversation.getUserId() != null && userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required to access this conversation");
        }
    }

    private void saveTurn(ChatConversationEntity conversation, String role, String content, List<ChatSourceDto> sources) {
        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRoleName(role);
        message.setContent(content);
        message.setSourcesJson(toSourcesJson(sources));
        messageRepository.save(message);
    }

    private String generateReply(
            ChatRequest request,
            RetrievalResult retrieval,
            boolean productIntent,
            ContextualProductReference productReference) {
        String latestQuestion = request.messages().get(request.messages().size() - 1).content();
        if (containsEmergencySignals(latestQuestion)) {
            return EMERGENCY_REPLY;
        }
        if (requiresStrictHumanFallback(latestQuestion, retrieval)) {
            return buildHighRiskFallback(latestQuestion);
        }
        if (productIntent) {
            return buildDeterministicProductReply(latestQuestion, retrieval, productReference);
        }

        List<LlmClient.Message> conversationMessages = new ArrayList<>();
        request.messages().forEach(message -> conversationMessages.add(new LlmClient.Message(message.role(), message.content())));

        if (llmClient.isReady()) {
            try {
                String aiReply = llmClient.chat(buildMedicalDirectSystemPrompt(), conversationMessages);
                if (aiReply != null && !aiReply.isBlank()) {
                    log.info("llm reply generated successfully");
                    return aiReply;
                }
            } catch (RuntimeException ex) {
                log.warn("llm generation failed, using fallback: {}", ex.getMessage(), ex);
            }
        }

        return buildRuleBasedMedicalFallback(latestQuestion);
    }

    private String buildMedicalDirectSystemPrompt() {
        return """
                You are a medical information assistant.
                Answer in natural Vietnamese with full accents.
                Keep the tone warm, calm, easy to understand, and friendly for everyday users.
                The user is usually asking about diseases, symptoms, causes, home care, warning signs, and when to seek care.
                You may answer from general medical knowledge even when no retrieval context is provided.
                Do not diagnose with certainty.
                Do not prescribe prescription-only medication.
                Do not give exact dosing for pregnancy, breastfeeding, children, elderly people, chronic disease, kidney disease, liver disease, or allergy history.
                If the question suggests emergency symptoms, tell the user to seek urgent medical care immediately.
                When the user asks what medicine to use, only give general over-the-counter examples if appropriate, mention safety cautions, and avoid definite prescribing.
                Prefer short sections with simple headings and bullet points when helpful.
                Keep answers practical and structured like this when possible:
                1. bệnh này là gì hoặc khả năng thường gặp là gì
                2. triệu chứng hoặc nguyên nhân thường gặp
                3. nên làm gì trước mắt
                4. khi nào cần đi khám
                5. nhắc ngắn rằng đây không thay thế tư vấn y tế trực tiếp.
                Do not answer in English unless the user explicitly asks.
                """;
    }

    private String buildDeterministicProductReply(
            String question,
            RetrievalResult retrieval,
            ContextualProductReference productReference) {
        String normalized = normalizeForIntent(question);
        if (retrieval.products().isEmpty()) {
            if (productReference.productId() != null && productReference.productTitle() != null) {
                return "Mình hiểu bạn đang hỏi tiếp về sản phẩm " + productReference.productTitle()
                        + ", nhưng lần tra cứu này mình chưa lấy lại được dữ liệu tồn kho tương ứng. Bạn có thể bấm nút xem chi tiết bên dưới hoặc gửi lại tên gần đúng của thuốc để mình dò lại ngay.";
            }
            return "Mình chưa tìm thấy sản phẩm phù hợp trong dữ liệu nhà thuốc cho câu hỏi này. Bạn có thể gửi tên thuốc gần đúng hơn, hoạt chất, dạng bào chế hoặc hỏi theo kiểu như \"paracetamol còn hàng không\" để mình dò chính xác hơn.";
        }

        var top = retrieval.products().get(0);
        StringBuilder builder = new StringBuilder();
        builder.append("Mình đã kiểm tra trong nhà thuốc. ");

        if (retrieval.products().size() == 1) {
            builder.append("Sản phẩm gần nhất là ").append(top.name()).append(". ");
        } else {
            builder.append("Mình tìm được ").append(Math.min(3, retrieval.products().size()))
                    .append(" sản phẩm gần nhất, ưu tiên là ").append(top.name()).append(". ");
        }

        if (normalized.contains("con hang") || normalized.contains("ton kho") || normalized.contains("co khong")) {
            builder.append("Tình trạng hiện tại của ").append(top.name()).append(": ")
                    .append(top.stockStatus()).append(", khả dụng ")
                    .append(top.available()).append(" sản phẩm. ");
        } else if (normalized.contains("gia") || normalized.contains("bao nhieu tien")) {
            builder.append("Giá tham khảo của ").append(top.name()).append(" là ")
                    .append(formatPrice(top.price())).append(". ");
        } else if (normalized.contains("thuoc nao")
                || normalized.contains("uog thuoc gi")
                || normalized.contains("uong thuoc gi")
                || normalized.contains("de xuat")
                || normalized.contains("goi y")) {
            builder.append("Mình đã ghép các sản phẩm liên quan đang có trong nhà thuốc để bạn tham khảo. ");
        } else {
            builder.append("Mình đã ghép thông tin tồn kho và sản phẩm liên quan để bạn tham khảo. ");
        }

        if (top.activeIngredient() != null && !top.activeIngredient().isBlank()) {
            builder.append("Hoạt chất chính: ").append(top.activeIngredient().trim()).append(". ");
        }
        if (top.indications() != null && !top.indications().isBlank()) {
            builder.append("Công dụng tham khảo: ").append(trimSentence(top.indications())).append(". ");
        }
        if (top.prescriptionRequired()) {
            builder.append("Lưu ý đây là nhóm thuốc cần dược sĩ hoặc bác sĩ xác nhận trước khi dùng. ");
        }
        if (retrieval.products().size() > 1) {
            builder.append("Bạn có thể xem các lựa chọn ngay bên dưới và bấm vào nút để mở trang chi tiết, sau đó đặt mua. ");
        } else {
            builder.append("Bạn có thể bấm nút xem chi tiết hoặc đặt mua ngay bên dưới. ");
        }
        return builder.toString().trim();
    }

    private boolean needsHumanSupport(String question, RetrievalResult retrieval) {
        return containsEmergencySignals(question)
                || isHighRiskQuestion(question)
                || retrieval.products().stream().anyMatch(product -> product.prescriptionRequired());
    }

    private String toSourcesJson(List<ChatSourceDto> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (JsonProcessingException ex) {
            log.warn("failed to serialize chat sources: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private UUID extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String trimTitle(String question) {
        if (question == null || question.isBlank()) {
            return "Cuộc trò chuyện mới";
        }
        String normalized = question.trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 117) + "...";
    }

    private String normalizeForIntent(String question) {
        if (question == null) {
            return "";
        }
        String lowered = question.toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d");
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildRetrievalQuery(ChatRequest request) {
        List<String> recentUserMessages = request.messages().stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .map(message -> message.content() == null ? "" : message.content().trim())
                .filter(message -> !message.isBlank())
                .skip(Math.max(
                        0,
                        request.messages().stream()
                                .filter(message -> "user".equalsIgnoreCase(message.role()))
                                .count() - 3))
                .toList();
        if (recentUserMessages.isEmpty()) {
            return "";
        }
        String latest = recentUserMessages.get(recentUserMessages.size() - 1);
        if (recentUserMessages.size() == 1 || latest.length() >= 30) {
            return latest;
        }
        return recentUserMessages.stream().collect(Collectors.joining(" | "));
    }

    private boolean containsEmergencySignals(String question) {
        String normalized = normalizeForIntent(question);
        return normalized.contains("kho tho")
                || normalized.contains("khong tho duoc")
                || normalized.contains("dau nguc")
                || normalized.contains("co giat")
                || normalized.contains("hon me")
                || normalized.contains("me mo")
                || normalized.contains("ngat")
                || normalized.contains("soc")
                || normalized.contains("dot quy")
                || normalized.contains("tay chan te")
                || normalized.contains("liet")
                || normalized.contains("chay mau nhieu")
                || normalized.contains("qua lieu");
    }

    private boolean isHighRiskQuestion(String question) {
        String normalized = normalizeForIntent(question);
        return normalized.contains("mang thai")
                || normalized.contains("cho con bu")
                || normalized.contains("tre em")
                || normalized.contains("tre so sinh")
                || normalized.contains("nguoi gia")
                || normalized.contains("benh nen")
                || normalized.contains("suy gan")
                || normalized.contains("suy than")
                || normalized.contains("di ung")
                || normalized.contains("phat ban")
                || normalized.contains("lieu bao nhieu")
                || normalized.contains("uong bao nhieu");
    }

    private boolean requiresStrictHumanFallback(String question, RetrievalResult retrieval) {
        return isHighRiskQuestion(question) && retrieval.products().isEmpty();
    }

    private String buildHighRiskFallback(String question) {
        if (containsEmergencySignals(question)) {
            return EMERGENCY_REPLY;
        }
        return "Đây là nhóm câu hỏi cần thận trọng hơn. Mình chỉ nên cung cấp thông tin tham khảo chung, không thể cá nhân hóa chẩn đoán hoặc đưa liều dùng cụ thể. Bạn nên liên hệ bác sĩ hoặc dược sĩ, đặc biệt nếu liên quan đến mang thai, cho con bú, trẻ nhỏ, người lớn tuổi, bệnh nền, dị ứng hoặc đang dùng nhiều thuốc cùng lúc.";
    }

    private String buildRuleBasedMedicalFallback(String question) {
        String normalized = normalizeForIntent(question);
        boolean definitionQuestion = normalized.contains("la gi")
                || normalized.contains("la sao")
                || normalized.contains("nghia la gi")
                || normalized.startsWith("bi ")
                || normalized.startsWith("toi bi ")
                || normalized.startsWith("em bi ");

        if (normalized.contains("viem xoang") || normalized.contains("xoang")) {
            if (definitionQuestion) {
                return "Viêm xoang là tình trạng niêm mạc các xoang quanh mũi bị viêm, thường xảy ra sau cảm lạnh, dị ứng hoặc viêm mũi kéo dài. Triệu chứng hay gặp là nghẹt mũi, chảy mũi, nặng mặt, đau vùng trán hoặc hai bên má, nhức đầu và giảm ngửi mùi. Bạn nên nghỉ ngơi, uống đủ nước, rửa mũi bằng nước muối sinh lý và theo dõi thêm. Nếu sốt cao, đau tăng nhiều, sưng quanh mắt, dịch mũi hôi hoặc kéo dài quá khoảng 7 đến 10 ngày không đỡ, bạn nên đi khám tai mũi họng. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
            }
            return "Nếu bạn đang bị viêm xoang, điều thường gặp là nghẹt mũi, chảy mũi, đau hoặc nặng vùng mặt, nhức đầu và giảm ngửi mùi. Bạn có thể nghỉ ngơi, uống đủ nước, rửa mũi bằng nước muối sinh lý và tránh khói bụi hoặc dị nguyên nếu có. Nếu đau nhiều, sốt cao, sưng quanh mắt, khó thở hoặc triệu chứng kéo dài không đỡ, bạn nên đi khám sớm. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
        }

        if (normalized.contains("dau dau") || normalized.contains("nhuc dau")) {
            if (normalized.contains("buoi toi") || normalized.contains("ban dem")) {
                return "Đau đầu buổi tối có thể liên quan đến căng thẳng sau một ngày làm việc, thiếu ngủ, mỏi mắt, dùng màn hình lâu, uống ít nước, viêm xoang hoặc đôi khi do huyết áp. Bạn nên thử nghỉ ngơi, uống đủ nước, giảm nhìn màn hình, ngủ đúng giờ và để ý xem cơn đau có xuất hiện sau khi làm việc lâu, học nhiều hay thức khuya không. Nếu đau đầu lặp lại nhiều ngày, đau tăng dần, kèm sốt, nôn, nhìn mờ, tê yếu tay chân hoặc đau quá dữ dội, bạn nên đi khám sớm. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
            }
            return "Đau đầu có thể liên quan đến căng thẳng, thiếu ngủ, viêm xoang, mỏi mắt, cảm cúm hoặc nhiều nguyên nhân khác. Bạn nên nghỉ ngơi, uống đủ nước, tránh thức khuya và theo dõi xem cơn đau xuất hiện lúc nào, kéo dài bao lâu, có kèm sốt, nôn, nhìn mờ hay tê yếu tay chân không. Nếu đau dữ dội, tái phát nhiều hoặc kèm dấu hiệu bất thường, bạn nên đi khám sớm. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
        }

        if (normalized.contains("nghet mui") || normalized.contains("so mui") || normalized.contains("chay mui")) {
            return "Nghẹt mũi hoặc sổ mũi thường gặp trong cảm lạnh, dị ứng, viêm mũi hoặc viêm xoang. Bạn nên uống đủ nước, rửa mũi bằng nước muối sinh lý và theo dõi xem có kèm sốt, đau mặt, đau họng hay khó thở không. Nếu kéo dài, tái phát nhiều hoặc có dịch mũi hôi, đau đầu tăng dần, bạn nên đi khám tai mũi họng. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
        }

        if (normalized.contains("ho") || normalized.contains("sot") || normalized.contains("dau hong")) {
            return "Các triệu chứng như ho, sốt hoặc đau họng thường gặp trong cảm lạnh, viêm họng, viêm mũi xoang hoặc nhiễm siêu vi đường hô hấp trên. Bạn nên nghỉ ngơi, uống đủ nước ấm và theo dõi thêm các dấu hiệu như khó thở, sốt cao kéo dài, đau ngực hoặc mệt nhiều. Nếu có các dấu hiệu này hoặc triệu chứng nặng dần, bạn nên đi khám. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
        }

        return "Mình có thể hỗ trợ theo hướng thông tin y khoa chung. Với triệu chứng bạn đang mô tả, cách an toàn nhất là theo dõi 4 điểm: triệu chứng chính là gì, kéo dài bao lâu, mức độ nặng ra sao và có kèm sốt, khó thở, đau ngực, nôn nhiều, nhìn mờ hoặc tê yếu tay chân không. Nếu chỉ là triệu chứng nhẹ mới xuất hiện, bạn nên nghỉ ngơi, uống đủ nước và theo dõi thêm. Nếu triệu chứng kéo dài, tái phát nhiều hoặc có dấu hiệu bất thường, bạn nên đi khám để được đánh giá trực tiếp. Thông tin này chỉ mang tính tham khảo, không thay thế tư vấn y tế trực tiếp.";
    }

    private boolean isProductIntent(String question, UUID requestProductId, ContextualProductReference productReference) {
        if (requestProductId != null || productReference.productId() != null) {
            return true;
        }
        String normalized = normalizeForIntent(question);
        boolean explicitCommerce = normalized.contains("mua")
                || normalized.contains("gia")
                || normalized.contains("bao nhieu tien")
                || normalized.contains("con hang")
                || normalized.contains("ton kho")
                || normalized.contains("nha thuoc co")
                || normalized.contains("dat hang");
        boolean explicitProductWords = normalized.contains("san pham")
                || normalized.contains("thuoc")
                || normalized.contains("hoat chat")
                || normalized.contains("vien")
                || normalized.contains("siro")
                || normalized.contains("capsule")
                || normalized.contains("tablet")
                || normalized.contains("thuoc nho")
                || normalized.contains("xit mui");
        boolean referential = normalized.contains("san pham do")
                || normalized.contains("san pham nay")
                || normalized.contains("thuoc do")
                || normalized.contains("thuoc nay")
                || normalized.contains("loai do")
                || normalized.contains("loai nay")
                || normalized.contains("cai do")
                || normalized.contains("cai nay");
        boolean recommendationStyle = normalized.contains("uong thuoc gi")
                || normalized.contains("thuoc nao")
                || normalized.contains("de xuat thuoc")
                || normalized.contains("goi y thuoc");
        return explicitCommerce
                || recommendationStyle
                || (explicitProductWords && (normalized.contains("co khong") || normalized.contains("loai nao")))
                || (referential && productReference.productId() != null);
    }

    private ContextualProductReference resolveContextualProductReference(UUID conversationId, String latestQuestion) {
        if (conversationId == null) {
            return ContextualProductReference.empty();
        }
        String normalized = normalizeForIntent(latestQuestion);
        boolean wantsReference = normalized.contains("do")
                || normalized.contains("nay")
                || normalized.contains("con khong")
                || normalized.contains("ton kho")
                || normalized.contains("co khong")
                || normalized.contains("san pham")
                || normalized.contains("thuoc");
        if (!wantsReference) {
            return ContextualProductReference.empty();
        }

        List<ChatMessageEntity> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageEntity message = messages.get(i);
            if (!"assistant".equalsIgnoreCase(message.getRoleName())) {
                continue;
            }
            List<ChatSourceDto> parsedSources = parseSources(message.getSourcesJson());
            for (ChatSourceDto source : parsedSources) {
                if ("product".equalsIgnoreCase(source.type()) && source.id() != null) {
                    return new ContextualProductReference(source.id(), source.title());
                }
            }
        }
        return ContextualProductReference.empty();
    }

    private List<ChatSourceDto> parseSources(String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) {
            return List.of();
        }
        try {
            List<ChatSourceDto> sources = objectMapper.readValue(sourcesJson, SOURCE_LIST_TYPE);
            return sources == null ? List.of() : sources;
        } catch (JsonProcessingException ex) {
            log.warn("failed to parse chat sources: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "chưa có dữ liệu giá" : price.stripTrailingZeros().toPlainString() + " VND";
    }

    private String trimSentence(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 177) + "...";
    }

    private record ContextualProductReference(UUID productId, String productTitle) {
        private static ContextualProductReference empty() {
            return new ContextualProductReference(null, null);
        }
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "chưa có dữ liệu" : value.trim();
    }
}
