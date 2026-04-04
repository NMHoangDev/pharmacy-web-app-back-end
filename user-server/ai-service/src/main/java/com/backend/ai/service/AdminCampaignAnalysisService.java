package com.backend.ai.service;

import com.backend.ai.api.dto.AdminCampaignAnalysisRequest;
import com.backend.ai.api.dto.AdminCampaignAnalysisResponse;
import com.backend.ai.client.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AdminCampaignAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AdminCampaignAnalysisService.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AdminCampaignAnalysisService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public AdminCampaignAnalysisResponse analyze(AdminCampaignAnalysisRequest request) {
        AdminCampaignAnalysisResponse fallback = buildFallback(request);

        if (!llmClient.isReady()) {
            return fallback;
        }

        try {
            String raw = llmClient.chat(
                    buildSystemPrompt(),
                    List.of(new LlmClient.Message("user", buildUserPrompt(request, fallback))));
            AdminCampaignAnalysisResponse parsed = parseResponse(raw, fallback);
            if (parsed != null) {
                return parsed;
            }
        } catch (RuntimeException ex) {
            log.warn("campaign analysis generation failed, using fallback: {}", ex.getMessage(), ex);
        }

        return fallback;
    }

    private AdminCampaignAnalysisResponse buildFallback(AdminCampaignAnalysisRequest request) {
        List<String> opportunities = buildOpportunities(request);
        List<String> risks = buildRisks(request);
        List<AdminCampaignAnalysisResponse.CampaignProposal> proposals = buildProposals(request);

        String headline = "AI đang gợi ý 3 phương án chiến dịch sát với nhịp bán hiện tại";
        String summary = String.format(
                Locale.ROOT,
                "Doanh thu hiện tại tại %s đang ở mức %.0f VND, biến động %.1f%% so với kỳ trước. Hệ thống thấy %d SKU nên nhập thêm, %d SKU có rủi ro hết hàng sớm và %d SKU quay vòng chậm, nên chiến dịch nên vừa kéo doanh thu vừa xử lý áp lực tồn kho.",
                safeText(request.branchLabel(), "chi nhánh đang chọn"),
                safeDouble(request.totalRevenue()),
                safeDouble(request.revenueChangePct()),
                sizeOf(request.forecast() == null ? null : request.forecast().restock()),
                sizeOf(request.forecast() == null ? null : request.forecast().stockout()),
                sizeOf(request.forecast() == null ? null : request.forecast().slowMoving()));

        return new AdminCampaignAnalysisResponse(
                headline,
                summary,
                opportunities,
                risks,
                proposals,
                false,
                llmClient.model());
    }

    private List<String> buildOpportunities(AdminCampaignAnalysisRequest request) {
        List<String> values = new ArrayList<>();
        List<AdminCampaignAnalysisRequest.ForecastItemSnapshot> restockItems =
                safeForecastItems(request.forecast() == null ? null : request.forecast().restock());
        if (!restockItems.isEmpty()) {
            values.add("Có nhóm SKU đang bán khỏe trong 7 ngày tới, phù hợp chạy campaign kéo doanh thu ngắn hạn trước khi nhu cầu giảm.");
        }

        List<AdminCampaignAnalysisRequest.ProductSnapshot> topProducts = safeTopProducts(request);
        if (!topProducts.isEmpty()) {
            String topNames = topProducts.stream()
                    .sorted(Comparator.comparingInt(this::productQuantity).reversed())
                    .limit(3)
                    .map(product -> safeText(product.name(), "Sản phẩm đang được quan tâm"))
                    .collect(Collectors.joining(", "));
            values.add("Nhóm sản phẩm được quan tâm cao hiện tại là " + topNames + ", phù hợp làm trục chính cho chiến dịch.");
        }

        if (safeDouble(request.onlineRevenue()) > safeDouble(request.posRevenue())) {
            values.add("Doanh thu online đang nhỉnh hơn tại quầy, nên ưu tiên nội dung push, banner và ưu đãi dễ chốt nhanh.");
        } else {
            values.add("Doanh thu tại quầy vẫn chiếm tỷ trọng cao, nên campaign nên hỗ trợ dược sĩ tư vấn và combo mua trực tiếp.");
        }
        return values;
    }

    private List<String> buildRisks(AdminCampaignAnalysisRequest request) {
        List<String> values = new ArrayList<>();
        List<AdminCampaignAnalysisRequest.InventoryAlertSnapshot> lowStockItems = safeInventoryAlerts(request);
        if (!lowStockItems.isEmpty()) {
            String names = lowStockItems.stream()
                    .sorted(Comparator.comparingInt(this::inventoryOnHand))
                    .limit(3)
                    .map(item -> safeText(item.name(), "SKU tồn kho thấp"))
                    .collect(Collectors.joining(", "));
            values.add("Một số SKU tồn kho thấp như " + names + " không nên bị đẩy mạnh quá mức nếu chưa bổ sung hàng.");
        }

        if (!safeForecastItems(request.forecast() == null ? null : request.forecast().slowMoving()).isEmpty()) {
            values.add("Có SKU quay vòng chậm, nếu giảm giá quá rộng sẽ làm loãng biên lợi nhuận thay vì xử lý đúng điểm tồn kho.");
        }

        if (safeDouble(request.revenueChangePct()) < -5) {
            values.add("Doanh thu đang giảm rõ so với kỳ trước, nên campaign cần ưu tiên chốt đơn nhanh và tránh cấu trúc ưu đãi quá phức tạp.");
        }
        return values;
    }

    private List<AdminCampaignAnalysisResponse.CampaignProposal> buildProposals(AdminCampaignAnalysisRequest request) {
        List<String> heroProducts = topProductNames(request, 3);
        List<String> slowProducts = forecastNames(request.forecast() == null ? null : request.forecast().slowMoving(), 3);
        List<String> restockProducts = forecastNames(request.forecast() == null ? null : request.forecast().restock(), 3);

        AdminCampaignAnalysisResponse.CampaignProposal proposal1 = new AdminCampaignAnalysisResponse.CampaignProposal(
                "campaign_velocity",
                "Bứt tốc đơn hàng cuối tuần",
                "Tăng doanh thu ngắn hạn trên nhóm đang bán tốt",
                "Chạy 3 ngày cuối tuần hoặc 72 giờ tới",
                "Phương án này bám nhóm sản phẩm bán khỏe để tối ưu xác suất chốt đơn ngay, phù hợp khi cần đẩy tăng trưởng nhanh mà không phải thay đổi quá nhiều vận hành.",
                "Giảm trực tiếp 8-12% cho nhóm chủ lực, kèm freeship theo ngưỡng đơn hoặc combo 2 sản phẩm.",
                "Kỳ vọng kéo doanh thu gần về mức dự báo tuần tới và tăng tỷ lệ chốt đơn trên nhóm sản phẩm chủ lực.",
                heroProducts,
                buildMarketing(
                        "Cuối tuần khỏe hơn cùng ưu đãi cho nhóm sản phẩm được mua nhiều nhất tại nhà thuốc.",
                        "Ưu đãi cuối tuần cho sản phẩm đang được mua nhiều. Mở app để xem mức giảm hôm nay.",
                        "Nha thuoc dang co uu dai cuoi tuan cho nhom san pham ban chay. Mo app de xem ngay.",
                        "Cuối tuần này, nhà thuốc gợi ý các sản phẩm đang được khách hàng quan tâm nhiều nhất cùng mức ưu đãi dễ chốt đơn. Phù hợp để mua nhanh và an tâm hơn."));

        AdminCampaignAnalysisResponse.CampaignProposal proposal2 = new AdminCampaignAnalysisResponse.CampaignProposal(
                "campaign_inventory",
                "Giải phóng tồn kho có kiểm soát",
                "Giảm áp lực hàng quay vòng chậm và giữ biên lợi nhuận",
                "Chạy 5-7 ngày trong tuần thường",
                "Phương án này tập trung xử lý các SKU quay vòng chậm nhưng vẫn giữ cấu trúc ưu đãi có kiểm soát để không làm ảnh hưởng toàn bộ biên lợi nhuận của danh mục.",
                "Ưu đãi theo combo hoặc mua kèm cho nhóm tồn kho cao, ưu tiên quà tặng nhỏ hoặc giảm nhẹ 5-8% thay vì giảm sâu toàn bộ.",
                "Kỳ vọng cải thiện vòng quay tồn kho, giải phóng chỗ kệ và giảm rủi ro giam vốn ở nhóm chậm bán.",
                slowProducts.isEmpty() ? heroProducts : slowProducts,
                buildMarketing(
                        "Ưu đãi chọn lọc cho các sản phẩm nên xoay vòng sớm tại nhà thuốc.",
                        "Nha thuoc dang co uu dai chon loc cho mot so san pham nen mua som trong tuan nay.",
                        "San pham duoc chon loc dang co uu dai nhe trong tuan nay. Xem ngay de tiet kiem.",
                        "Một số sản phẩm đang có ưu đãi chọn lọc để giúp khách hàng mua dễ hơn và nhà thuốc tối ưu vòng quay tồn kho. Đây là lúc phù hợp để gom các món cần thiết với chi phí hợp lý."));

        AdminCampaignAnalysisResponse.CampaignProposal proposal3 = new AdminCampaignAnalysisResponse.CampaignProposal(
                "campaign_balance",
                "Giữ nhịp tăng trưởng an toàn",
                "Cân bằng doanh thu, tồn kho và khả năng phục vụ",
                "Chạy 7 ngày với lịch truyền thông chia 2 đợt",
                "Phương án này cân bằng giữa sản phẩm bán tốt và sản phẩm cần ưu tiên xoay vòng, phù hợp khi admin muốn một chiến dịch bền hơn và ít rủi ro hụt hàng.",
                "Giảm giá nhẹ cho nhóm chủ lực, thêm combo mua kèm cho nhóm đang cần đẩy và ưu tiên hiển thị theo từng kênh online/POS.",
                "Kỳ vọng giữ doanh thu ổn định trong cả tuần, tránh hụt hàng ở nhóm nóng và đồng thời cải thiện chỉ số tồn kho.",
                mergeNames(heroProducts, restockProducts),
                buildMarketing(
                        "Chiến dịch cân bằng giúp mua đúng nhu cầu, tối ưu chi phí và vẫn đảm bảo hàng sẵn.",
                        "Nha thuoc dang co goi uu dai can bang cho nhom san pham duoc quan tam va nhom can day ton kho.",
                        "Uu dai can bang cho nhieu nhom san pham dang co san tai nha thuoc. Mo app de xem.",
                        "Chiến dịch tuần này được thiết kế để cân bằng giữa nhu cầu mua thực tế và tình hình tồn kho tại nhà thuốc. Admin có thể dùng phương án này khi muốn tăng trưởng ổn định thay vì tăng sốc ngắn hạn."));

        return List.of(proposal1, proposal2, proposal3);
    }

    private AdminCampaignAnalysisResponse.MarketingContent buildMarketing(
            String banner,
            String push,
            String sms,
            String social) {
        return new AdminCampaignAnalysisResponse.MarketingContent(banner, push, sms, social);
    }

    private String buildSystemPrompt() {
        return """
                Bạn là AI strategist cho một hệ thống nhà thuốc tại Việt Nam.
                Nhiệm vụ: đọc snapshot dashboard, sau đó trả về JSON duy nhất, không markdown, không giải thích ngoài JSON.
                Hãy viết toàn bộ bằng tiếng Việt có dấu, tự nhiên, sát vận hành nhà thuốc.
                Không bịa số liệu ngoài snapshot.
                Phải ưu tiên tính thực tế: doanh thu, tồn kho, nhóm bán chạy, nhóm chậm quay vòng, khác biệt online/POS.
                Hãy tạo:
                - headline: 1 câu ngắn
                - summary: 2-4 câu
                - opportunities: 3 ý
                - risks: 2-3 ý
                - proposals: đúng 3 phương án chiến dịch
                Mỗi proposal gồm:
                {
                  "id": "string",
                  "title": "string",
                  "objective": "string",
                  "timing": "string",
                  "rationale": "string",
                  "offerStructure": "string",
                  "expectedImpact": "string",
                  "recommendedProducts": ["..."],
                  "marketing": {
                    "banner": "string",
                    "pushNotification": "string",
                    "sms": "string",
                    "socialPost": "string"
                  }
                }
                Giữ nội dung gọn, cụ thể, có thể dùng ngay cho admin.
                """;
    }

    private String buildUserPrompt(AdminCampaignAnalysisRequest request, AdminCampaignAnalysisResponse fallback) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new PromptPayload(request, fallback));
        } catch (JsonProcessingException ex) {
            return "branch=" + safeText(request.branchLabel(), "chi nhánh") + ", revenue=" + safeDouble(request.totalRevenue());
        }
    }

    private AdminCampaignAnalysisResponse parseResponse(String raw, AdminCampaignAnalysisResponse fallback) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = stripMarkdownFence(raw.trim());
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            List<String> opportunities = readStringList(node.get("opportunities"), fallback.opportunities());
            List<String> risks = readStringList(node.get("risks"), fallback.risks());
            List<AdminCampaignAnalysisResponse.CampaignProposal> proposals = readProposals(node.get("proposals"), fallback.proposals());

            return new AdminCampaignAnalysisResponse(
                    readText(node, "headline", fallback.headline()),
                    readText(node, "summary", fallback.summary()),
                    opportunities,
                    risks,
                    proposals,
                    true,
                    llmClient.model());
        } catch (JsonProcessingException ex) {
            log.warn("failed to parse campaign analysis JSON: {}", ex.getMessage());
            return null;
        }
    }

    private List<AdminCampaignAnalysisResponse.CampaignProposal> readProposals(
            JsonNode node,
            List<AdminCampaignAnalysisResponse.CampaignProposal> fallback) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return fallback;
        }
        List<AdminCampaignAnalysisResponse.CampaignProposal> proposals = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode item = node.get(i);
            AdminCampaignAnalysisResponse.CampaignProposal fallbackItem = fallback.get(Math.min(i, fallback.size() - 1));
            JsonNode marketingNode = item.get("marketing");
            AdminCampaignAnalysisResponse.MarketingContent marketing = new AdminCampaignAnalysisResponse.MarketingContent(
                    readText(marketingNode, "banner", fallbackItem.marketing().banner()),
                    readText(marketingNode, "pushNotification", fallbackItem.marketing().pushNotification()),
                    readText(marketingNode, "sms", fallbackItem.marketing().sms()),
                    readText(marketingNode, "socialPost", fallbackItem.marketing().socialPost()));
            proposals.add(new AdminCampaignAnalysisResponse.CampaignProposal(
                    readText(item, "id", fallbackItem.id()),
                    readText(item, "title", fallbackItem.title()),
                    readText(item, "objective", fallbackItem.objective()),
                    readText(item, "timing", fallbackItem.timing()),
                    readText(item, "rationale", fallbackItem.rationale()),
                    readText(item, "offerStructure", fallbackItem.offerStructure()),
                    readText(item, "expectedImpact", fallbackItem.expectedImpact()),
                    readStringList(item.get("recommendedProducts"), fallbackItem.recommendedProducts()),
                    marketing));
        }
        if (proposals.size() < 3) {
            proposals.addAll(fallback.subList(proposals.size(), fallback.size()));
        }
        return proposals.stream().limit(3).toList();
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String text = item == null ? "" : item.asText("").trim();
            if (!text.isBlank()) {
                values.add(text);
            }
        });
        return values.isEmpty() ? fallback : values;
    }

    private String readText(JsonNode node, String field, String fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return fallback;
        }
        String value = fieldNode.asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private String stripMarkdownFence(String raw) {
        String cleaned = raw;
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z0-9]*\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        return cleaned.trim();
    }

    private List<String> topProductNames(AdminCampaignAnalysisRequest request, int limit) {
        List<AdminCampaignAnalysisRequest.ProductSnapshot> items = safeTopProducts(request);
        if (items.isEmpty()) {
            return List.of("Nhóm sản phẩm bán tốt hiện tại");
        }
        List<String> values = items.stream()
                .sorted(Comparator.comparingInt(this::productQuantity).reversed())
                .limit(limit)
                .map(product -> safeText(product.name(), "Sản phẩm bán tốt"))
                .filter(name -> !name.isBlank())
                .toList();
        return values.isEmpty() ? List.of("Nhóm sản phẩm bán tốt hiện tại") : values;
    }

    private List<String> forecastNames(List<AdminCampaignAnalysisRequest.ForecastItemSnapshot> items, int limit) {
        List<AdminCampaignAnalysisRequest.ForecastItemSnapshot> safeItems = safeForecastItems(items);
        if (safeItems.isEmpty()) {
            return List.of();
        }
        return safeItems.stream()
                .limit(limit)
                .map(item -> safeText(item.name(), "Sản phẩm cần lưu ý"))
                .filter(name -> !name.isBlank())
                .toList();
    }

    private List<String> mergeNames(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>();
        merged.addAll(first);
        for (String item : second) {
            if (item != null && !item.isBlank() && !merged.contains(item)) {
                merged.add(item);
            }
        }
        return merged.stream().limit(5).toList();
    }

    private int sizeOf(List<?> items) {
        return items == null ? 0 : (int) items.stream().filter(java.util.Objects::nonNull).count();
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private int productQuantity(AdminCampaignAnalysisRequest.ProductSnapshot item) {
        return item == null || item.quantity() == null ? 0 : item.quantity();
    }

    private int inventoryOnHand(AdminCampaignAnalysisRequest.InventoryAlertSnapshot item) {
        return item == null || item.onHand() == null ? 0 : item.onHand();
    }

    private List<AdminCampaignAnalysisRequest.ProductSnapshot> safeTopProducts(AdminCampaignAnalysisRequest request) {
        if (request == null || request.topProducts() == null) {
            return List.of();
        }
        return request.topProducts().stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<AdminCampaignAnalysisRequest.InventoryAlertSnapshot> safeInventoryAlerts(AdminCampaignAnalysisRequest request) {
        if (request == null || request.lowStockItems() == null) {
            return List.of();
        }
        return request.lowStockItems().stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<AdminCampaignAnalysisRequest.ForecastItemSnapshot> safeForecastItems(
            List<AdminCampaignAnalysisRequest.ForecastItemSnapshot> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record PromptPayload(
            AdminCampaignAnalysisRequest snapshot,
            AdminCampaignAnalysisResponse fallbackSuggestion) {
    }
}
