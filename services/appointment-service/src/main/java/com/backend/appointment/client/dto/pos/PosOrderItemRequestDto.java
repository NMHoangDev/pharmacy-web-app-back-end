package com.backend.appointment.client.dto.pos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PosOrderItemRequestDto(
        @NotNull UUID productId,
        String sku,
        String productName,
        String batchNo,
        String expiryDate,
        @NotNull @Min(1) Integer qty,
        @NotNull @Min(0) Long unitPrice) {
}
