package com.backend.ai.service;

import com.backend.ai.api.dto.DrugDraftRequest;
import com.backend.ai.api.dto.DrugDraftResponse;
import com.backend.ai.client.LlmClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminDrugDraftService {

    private static final Logger log = LoggerFactory.getLogger(AdminDrugDraftService.class);

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AdminDrugDraftService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public DrugDraftResponse generateDraft(DrugDraftRequest request) {
        String normalizedName = request.name() == null ? "" : request.name().trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Drug name is required");
        }

        if (llmClient.isReady()) {
            try {
                String raw = llmClient.chatJson(
                        buildSystemPrompt(),
                        List.of(new LlmClient.Message("user", normalizedName)),
                        1200);
                DrugDraftResponse parsed = parseDraftResponse(raw, normalizedName);
                if (parsed != null) {
                    return parsed;
                }
            } catch (RuntimeException ex) {
                log.warn("drug draft generation failed, using fallback: {}", ex.getMessage(), ex);
            }
        }

        return fallbackDraft(normalizedName);
    }

    private String buildSystemPrompt() {
        return """
                You are assisting an admin user who is creating a medicine product in a pharmacy catalog.
                The draft must be generated from general medical/pharmaceutical knowledge based on the provided drug name only.
                You must NOT retrieve, infer, or reference any internal pharmacy data such as inventory, branch, stock systems, catalog IDs, or past records.
                Return JSON only. Do not add markdown fences. Do not add explanations.
                Write all text fields in natural Vietnamese with full accents.
                Generate a practical product draft from the drug name only.
                Use cautious, generic medical information. Do not invent unsafe dosing certainty.
                If information is uncertain, still provide a reasonable short placeholder instead of leaving required fields empty.
                JSON schema:
                {
                  "name": "string",
                  "sku": "string",
                  "categoryHint": "string",
                  "costPrice": 0,
                  "salePrice": 0,
                  "stock": 0,
                  "status": "ACTIVE",
                  "prescriptionRequired": false,
                  "description": "string",
                  "dosageForm": "string",
                  "packaging": "string",
                  "activeIngredient": "string",
                  "indications": "string",
                  "usageDosage": "string",
                  "contraindicationsWarning": "string",
                  "otherInformation": "string",
                  "imageUrl": ""
                }
                Rules:
                - costPrice, salePrice, stock can be 0 when unknown.
                - Never claim real-time availability or branch stock; stock should stay 0 unless explicitly known.
                - categoryHint should be a short category label suggestion such as "Thuốc giảm đau", "Kháng sinh", "Vitamin", "Thuốc cảm cúm".
                - sku should be compact uppercase ASCII with dashes.
                - status should be ACTIVE.
                - description should be 1-2 sentences.
                - usageDosage must stay general and cautious.
                - contraindicationsWarning should mention key warnings briefly.
                - otherInformation can include bảo quản or lưu ý thêm.
                """;
    }

    private DrugDraftResponse parseDraftResponse(String raw, String fallbackName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = stripMarkdownFence(raw.trim());
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            return new DrugDraftResponse(
                    textOrDefault(node, "name", fallbackName),
                    textOrDefault(node, "sku", buildSku(fallbackName)),
                    textOrDefault(node, "categoryHint", "Thuốc"),
                    decimalOrDefault(node, "costPrice", BigDecimal.ZERO),
                    decimalOrDefault(node, "salePrice", BigDecimal.ZERO),
                    intOrDefault(node, "stock", 0),
                    textOrDefault(node, "status", "ACTIVE"),
                    boolOrDefault(node, "prescriptionRequired", false),
                    textOrDefault(node, "description", fallbackDescription(fallbackName)),
                    textOrDefault(node, "dosageForm", ""),
                    textOrDefault(node, "packaging", ""),
                    textOrDefault(node, "activeIngredient", ""),
                    textOrDefault(node, "indications", fallbackIndications(fallbackName)),
                    textOrDefault(node, "usageDosage", fallbackUsageDosage()),
                    textOrDefault(node, "contraindicationsWarning", fallbackWarning()),
                    textOrDefault(node, "otherInformation", "Bảo quản nơi khô ráo, tránh ánh sáng trực tiếp."),
                    textOrDefault(node, "imageUrl", ""));
        } catch (JsonProcessingException ex) {
            log.warn("failed to parse generated drug draft JSON: {}", ex.getMessage());
            return null;
        }
    }

    private DrugDraftResponse fallbackDraft(String name) {
        return new DrugDraftResponse(
                name,
                buildSku(name),
                "Thuốc",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                "ACTIVE",
                false,
                fallbackDescription(name),
                "",
                "",
                inferActiveIngredient(name),
                fallbackIndications(name),
                fallbackUsageDosage(),
                fallbackWarning(),
                "Bảo quản nơi khô ráo, tránh ánh sáng trực tiếp và để xa tầm tay trẻ em.",
                "");
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

    private BigDecimal decimalOrDefault(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        try {
            return new BigDecimal(value.asText()).max(BigDecimal.ZERO);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Integer intOrDefault(JsonNode node, String field, int fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return Math.max(0, value.asInt(fallback));
    }

    private Boolean boolOrDefault(JsonNode node, String field, boolean fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        return value.asBoolean(fallback);
    }

    private String buildSku(String name) {
        String ascii = normalizeAscii(name).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-");
        String compact = ascii.replaceAll("(^-|-$)", "");
        if (compact.isBlank()) {
            return "DRUG-AI";
        }
        if (compact.length() > 18) {
            compact = compact.substring(0, 18).replaceAll("-+$", "");
        }
        return compact;
    }

    private String normalizeAscii(String value) {
        String lowered = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lowered, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "")
                .replace("đ", "d")
                .replace("Đ", "d");
    }

    private String inferActiveIngredient(String name) {
        String normalized = normalizeAscii(name);
        if (normalized.contains("paracetamol") || normalized.contains("hapacol") || normalized.contains("efferalgan")) {
            return "Paracetamol";
        }
        if (normalized.contains("amoxicillin") || normalized.contains("augmentin")) {
            return "Amoxicillin";
        }
        if (normalized.contains("vitamin c")) {
            return "Vitamin C";
        }
        return "";
    }

    private String fallbackDescription(String name) {
        return name + " là sản phẩm dùng trong nhà thuốc. Nội dung chi tiết cần được dược sĩ rà soát lại trước khi lưu chính thức.";
    }

    private String fallbackIndications(String name) {
        return "Công dụng tham khảo của " + name + " cần được đối chiếu lại theo thông tin chuyên môn và tờ hướng dẫn sử dụng chính thức.";
    }

    private String fallbackUsageDosage() {
        return "Sử dụng theo hướng dẫn trên bao bì hoặc theo tư vấn của bác sĩ, dược sĩ. Không tự ý dùng quá liều khuyến cáo.";
    }

    private String fallbackWarning() {
        return "Thận trọng ở người có tiền sử dị ứng thuốc, phụ nữ mang thai, cho con bú, trẻ nhỏ hoặc người có bệnh nền. Cần dược sĩ rà soát lại trước khi bán.";
    }
}
