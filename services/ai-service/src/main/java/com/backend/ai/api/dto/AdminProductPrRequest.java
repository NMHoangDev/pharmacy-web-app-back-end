package com.backend.ai.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AdminProductPrRequest(
        @NotBlank String name,
        String categoryName,
        String shortDescription,
        String dosageForm,
        String packaging,
        String activeIngredient,
        String indications,
        String usageDosage,
        String contraindicationsWarning,
        String otherInformation,
        Boolean prescriptionRequired,
        BigDecimal salePrice,
        String toneHint,
        String campaignGoal) {
}
