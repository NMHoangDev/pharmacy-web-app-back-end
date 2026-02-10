package com.backend.inventory.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockDocumentLineRequest(
        @NotNull UUID productId,
        @NotNull Integer quantity,
        BigDecimal unitCost,
        String skuSnapshot,
        String batchNo,
        LocalDate expiryDate) {
}
