package com.backend.inventory.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockDocumentLineResponse(
        UUID id,
        UUID productId,
        String skuSnapshot,
        int quantity,
        BigDecimal unitCost,
        String batchNo,
        LocalDate expiryDate) {
}
