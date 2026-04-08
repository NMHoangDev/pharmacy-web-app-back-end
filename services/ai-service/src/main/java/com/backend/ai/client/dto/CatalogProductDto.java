package com.backend.ai.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProductDto(
        UUID id,
        String sku,
        String name,
        String slug,
        UUID categoryId,
        BigDecimal price,
        String status,
        boolean prescriptionRequired,
        String description,
        String dosageForm,
        String packaging,
        String activeIngredient,
        String indications,
        String usageDosage,
        String contraindicationsWarning,
        String otherInformation,
        String imageUrl,
        String attributes) {
}
