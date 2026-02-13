package com.backend.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DrugPublicDto(
        UUID id,
        String sku,
        String name,
        String slug,
        UUID categoryId,
        BigDecimal price,
        String status,
        boolean prescriptionRequired,
        String description,
        String imageUrl,
        String attributes) {
}
