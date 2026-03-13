package com.backend.pharmacist.api.dto.pos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PosOrderItemRequest(
        @NotNull UUID productId,
        String sku,
        String productName,
        String batchNo,
        LocalDate expiryDate,
        @NotNull @Min(1) Integer qty,
        @NotNull @Min(0) Long unitPrice) {
}
