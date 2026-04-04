package com.backend.catalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DrugAdminDto(
        UUID id,
        String sku,
        String name,
        String slug,
        UUID categoryId,
        BigDecimal costPrice,
        BigDecimal baseSalePrice,
        BigDecimal priceOverride,
        BigDecimal effectivePrice,
        String globalStatus,
        String branchStatus,
        String effectiveStatus,
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
        String attributes,
        UUID branchId,
        String note) {
}
