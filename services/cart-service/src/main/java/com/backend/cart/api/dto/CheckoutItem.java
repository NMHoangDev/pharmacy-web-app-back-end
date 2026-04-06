package com.backend.cart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutItem(
        @NotNull UUID productId,
        @NotBlank String productName,
        @Min(0) double unitPrice,
        @Min(1) int quantity) {
}
