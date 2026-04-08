package com.backend.order.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutItem(@NotNull UUID productId,
        @NotBlank String productName,
        @Min(1) int quantity,
        @Min(0) double unitPrice) {
}
