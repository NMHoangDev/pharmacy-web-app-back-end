package com.backend.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record DrugRequest(
                @NotBlank String sku,
                @NotBlank String name,
                @NotBlank String slug,
                @NotNull UUID categoryId,
                @NotNull BigDecimal costPrice,
                @NotNull BigDecimal salePrice,
                @NotBlank String status,
                Boolean prescriptionRequired,
                String description,
                String imageUrl,
                String attributes) {
}
