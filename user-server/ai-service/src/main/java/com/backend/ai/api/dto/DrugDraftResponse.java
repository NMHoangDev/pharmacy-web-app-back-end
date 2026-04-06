package com.backend.ai.api.dto;

import java.math.BigDecimal;

public record DrugDraftResponse(
        String name,
        String sku,
        String categoryHint,
        BigDecimal costPrice,
        BigDecimal salePrice,
        Integer stock,
        String status,
        Boolean prescriptionRequired,
        String description,
        String dosageForm,
        String packaging,
        String activeIngredient,
        String indications,
        String usageDosage,
        String contraindicationsWarning,
        String otherInformation,
        String imageUrl) {
}
