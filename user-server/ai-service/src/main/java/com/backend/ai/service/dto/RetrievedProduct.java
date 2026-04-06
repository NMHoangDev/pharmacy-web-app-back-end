package com.backend.ai.service.dto;

import com.backend.ai.api.dto.ChatSourceDto;

import java.math.BigDecimal;
import java.util.UUID;

public record RetrievedProduct(
        UUID id,
        String name,
        String slug,
        BigDecimal price,
        String description,
        String dosageForm,
        String packaging,
        String activeIngredient,
        String indications,
        String usageDosage,
        String contraindicationsWarning,
        String otherInformation,
        boolean prescriptionRequired,
        int available,
        int totalOnHand,
        String stockStatus) {

    public ChatSourceDto toSource() {
        String snippet = firstNonBlank(indications, description, usageDosage, contraindicationsWarning, otherInformation);
        return new ChatSourceDto(
                "product",
                id,
                name,
                trimSnippet(snippet),
                stockStatus,
                available,
                price,
                "catalog",
                null,
                "patient",
                prescriptionRequired ? "medium" : "low",
                prescriptionRequired ? "Can duoc si hoac bac si xac nhan truoc khi dung." : null);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String trimSnippet(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 217) + "...";
    }
}
