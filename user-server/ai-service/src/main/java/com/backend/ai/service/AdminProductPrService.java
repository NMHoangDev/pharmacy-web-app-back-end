package com.backend.ai.service;

import com.backend.ai.api.dto.AdminProductPrRequest;
import com.backend.ai.api.dto.AdminProductPrResponse;
import com.backend.ai.client.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminProductPrService {

    private static final Logger log = LoggerFactory.getLogger(AdminProductPrService.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AdminProductPrService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public AdminProductPrResponse generate(AdminProductPrRequest request) {
        String name = safeText(request.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (llmClient.isReady()) {
            try {
                String raw = llmClient.chat(buildSystemPrompt(), List.of(new LlmClient.Message("user", buildUserPrompt(request))));
                AdminProductPrResponse parsed = parseResponse(raw);
                if (parsed != null) {
                    return parsed;
                }
            } catch (RuntimeException ex) {
                log.warn("product PR generation failed, using fallback: {}", ex.getMessage(), ex);
            }
        }

        return fallback(request);
    }

    private String buildSystemPrompt() {
        return """
                You are helping an admin write a pharmacy content draft to review before publishing.
                Return JSON only. Do not add markdown fences. Do not add explanations.
                Write all natural-language fields in Vietnamese with full accents.
                The writing style must feel informative, warm, trustworthy, and subtle.
                Do not sound like a hard-sell advertisement. Avoid exaggerated claims.
                Keep medical language cautious. Do not promise treatment outcomes.
                The article is for product PR/content marketing review, not a prescription sheet.
                Output schema:
                {
                  "title": "string",
                  "excerpt": "string",
                  "caption": "string",
                  "contentHtml": "string",
                  "suggestedTags": ["string"],
                  "disclaimer": "string"
                }
                Rules:
                - title should be complete, attractive, and editorial in tone.
                - excerpt should be 1-2 sentences.
                - caption should invite readers into the article naturally and should not sound obviously promotional.
                - contentHtml must be valid HTML using only <h2>, <h3>, <p>, <ul>, <li>, <strong>.
                - contentHtml should have an intro, 3-4 sections, and a gentle closing.
                - suggestedTags should contain 3 to 5 short tags.
                - disclaimer should be short and cautious.
                """;
    }

    private String buildUserPrompt(AdminProductPrRequest request) {
        return """
                Tạo bản nháp bài viết PR mềm cho sản phẩm sau:
                - Tên sản phẩm: %s
                - Danh mục: %s
                - Mô tả ngắn: %s
                - Dạng bào chế: %s
                - Quy cách: %s
                - Hoạt chất: %s
                - Chỉ định/Công dụng tham khảo: %s
                - Cách dùng tham khảo: %s
                - Cảnh báo/chống chỉ định: %s
                - Thông tin khác: %s
                - Cần kê đơn: %s
                - Giá bán: %s
                - Tông mong muốn: %s
                - Mục tiêu chiến dịch: %s
                """.formatted(
                safeText(request.name()),
                fallbackText(request.categoryName(), "Sản phẩm chăm sóc sức khỏe"),
                fallbackText(request.shortDescription(), "Chưa có mô tả chi tiết"),
                fallbackText(request.dosageForm(), "Chưa rõ"),
                fallbackText(request.packaging(), "Chưa rõ"),
                fallbackText(request.activeIngredient(), "Chưa rõ"),
                fallbackText(request.indications(), "Cần rà soát thêm"),
                fallbackText(request.usageDosage(), "Khuyến nghị người dùng tham khảo tư vấn dược sĩ"),
                fallbackText(request.contraindicationsWarning(), "Cần đọc kỹ hướng dẫn sử dụng"),
                fallbackText(request.otherInformation(), "Không có thêm ghi chú"),
                Boolean.TRUE.equals(request.prescriptionRequired()) ? "Có" : "Không",
                request.salePrice() == null ? "Chưa xác định" : request.salePrice().stripTrailingZeros().toPlainString() + " VND",
                fallbackText(request.toneHint(), "gần gũi, đáng tin cậy, tinh tế"),
                fallbackText(request.campaignGoal(), "giúp admin có một bản nháp bài viết để biên tập và review"));
    }

    private AdminProductPrResponse parseResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(stripMarkdownFence(raw.trim()));
            String title = textOrDefault(node, "title", "");
            String excerpt = textOrDefault(node, "excerpt", "");
            String caption = textOrDefault(node, "caption", "");
            String contentHtml = textOrDefault(node, "contentHtml", "");
            if (title.isBlank() || excerpt.isBlank() || caption.isBlank() || contentHtml.isBlank()) {
                return null;
            }
            return new AdminProductPrResponse(
                    title,
                    excerpt,
                    caption,
                    contentHtml,
                    tagsOrDefault(node.get("suggestedTags")),
                    textOrDefault(node, "disclaimer",
                            "Nội dung mang tính tham khảo, nên được dược sĩ hoặc bác sĩ rà soát trước khi đăng chính thức."),
                    true,
                    llmClient.model());
        } catch (JsonProcessingException ex) {
            log.warn("failed to parse generated PR JSON: {}", ex.getMessage());
            return null;
        }
    }

    private AdminProductPrResponse fallback(AdminProductPrRequest request) {
        String name = safeText(request.name());
        String category = fallbackText(request.categoryName(), "sản phẩm chăm sóc sức khỏe");
        String activeIngredient = fallbackText(request.activeIngredient(), "thành phần phù hợp");
        String indications = fallbackText(request.indications(), "nhu cầu chăm sóc sức khỏe hằng ngày");
        String dosageForm = fallbackText(request.dosageForm(), "dạng dùng tiện lợi");
        String usage = fallbackText(request.usageDosage(),
                "Người dùng nên đọc kỹ hướng dẫn sử dụng và tham khảo tư vấn chuyên môn khi cần.");
        String warning = fallbackText(request.contraindicationsWarning(),
                "Cần thận trọng với người có cơ địa nhạy cảm hoặc đang điều trị bệnh nền.");

        String title = "Khám phá " + name + ": lựa chọn đáng lưu ý khi cần " + toLowerSentence(indications);
        String excerpt = name + " là " + category + " được nhiều người quan tâm nhờ " + dosageForm
                + " tiện dùng và định hướng hỗ trợ phù hợp với nhu cầu thường gặp. Bản nháp này giúp đội ngũ nội dung có sẵn khung bài để rà soát và biên tập thêm trước khi xuất bản.";
        String caption = "Nếu bạn đang tìm một sản phẩm dễ tiếp cận để tham khảo thêm trong nhóm " + category.toLowerCase(Locale.ROOT)
                + ", bài viết này sẽ giúp bạn nhìn nhanh vào điểm nổi bật, cách dùng tham khảo và các lưu ý cần biết của " + name + ".";
        String contentHtml = """
                <h2>%s có gì đáng chú ý?</h2>
                <p>%s là %s, nổi bật với %s và định hướng hỗ trợ %s. Đây là một bản nháp nội dung theo hướng chia sẻ thông tin, giúp đội ngũ biên tập dễ dàng hoàn thiện bài PR mềm cho sản phẩm.</p>
                <h2>Điểm nổi bật để triển khai nội dung</h2>
                <ul>
                  <li><strong>Dạng dùng:</strong> %s</li>
                  <li><strong>Thành phần tham khảo:</strong> %s</li>
                  <li><strong>Quy cách:</strong> %s</li>
                </ul>
                <h2>Phù hợp với nhu cầu nào?</h2>
                <p>%s có thể được nhắc đến trong bối cảnh người dùng đang tìm giải pháp phù hợp cho %s. Khi biên tập bài chính thức, đội ngũ nội dung nên giữ giọng điệu gần gũi, giải thích rõ lợi ích thực tế và tránh đưa ra khẳng định tuyệt đối.</p>
                <h2>Cách dùng và lưu ý khi truyền thông</h2>
                <p>%s</p>
                <p>%s</p>
                <h3>Kết bài gợi mở</h3>
                <p>Thay vì nhấn mạnh bán hàng, bài viết nên tập trung vào trải nghiệm sử dụng, sự tiện lợi và những thông tin quan trọng mà người đọc cần nắm trước khi lựa chọn %s.</p>
                """.formatted(
                name,
                name,
                category,
                dosageForm,
                toLowerSentence(indications),
                dosageForm,
                activeIngredient,
                fallbackText(request.packaging(), "Đang cập nhật"),
                name,
                toLowerSentence(indications),
                usage,
                warning,
                name);

        return new AdminProductPrResponse(
                title,
                excerpt,
                caption,
                contentHtml,
                buildFallbackTags(request),
                "Nội dung mang tính tham khảo, cần được dược sĩ hoặc bác sĩ rà soát trước khi sử dụng cho mục đích truyền thông chính thức.",
                false,
                "fallback");
    }

    private List<String> buildFallbackTags(AdminProductPrRequest request) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add("Tư vấn sản phẩm");
        if (!safeText(request.categoryName()).isBlank()) {
            tags.add(request.categoryName().trim());
        }
        if (!safeText(request.dosageForm()).isBlank()) {
            tags.add(request.dosageForm().trim());
        }
        if (!safeText(request.activeIngredient()).isBlank()) {
            String ingredient = request.activeIngredient().trim();
            tags.add(ingredient.length() > 36 ? ingredient.substring(0, 36).trim() : ingredient);
        }
        tags.add(Boolean.TRUE.equals(request.prescriptionRequired()) ? "Thuốc kê đơn" : "Chăm sóc sức khỏe");
        return new ArrayList<>(tags).stream().limit(5).toList();
    }

    private List<String> tagsOrDefault(JsonNode node) {
        List<String> tags = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    tags.add(value);
                }
            });
        }
        if (!tags.isEmpty()) {
            return tags.stream().distinct().limit(5).toList();
        }
        return List.of("Tư vấn sản phẩm", "Chăm sóc sức khỏe", "Bản nháp PR");
    }

    private String stripMarkdownFence(String raw) {
        String cleaned = raw;
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        String text = value.asText("").trim();
        return text.isBlank() ? fallback : text;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallbackText(String value, String fallback) {
        String safe = safeText(value);
        return safe.isBlank() ? fallback : safe;
    }

    private String toLowerSentence(String value) {
        String safe = safeText(value);
        if (safe.isBlank()) {
            return "nhu cầu chăm sóc sức khỏe thường gặp";
        }
        return safe.substring(0, 1).toLowerCase(Locale.ROOT) + safe.substring(1);
    }

    @SuppressWarnings("unused")
    private String normalizeAscii(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
