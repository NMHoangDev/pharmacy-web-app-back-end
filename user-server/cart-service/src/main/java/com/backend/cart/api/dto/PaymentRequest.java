package com.backend.cart.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentRequest(
                @NotNull UUID orderId,
                @NotNull UUID reservationId,
                @NotBlank String provider,
                @NotBlank String transactionRef,
                @Min(0) double amount,
                UUID branchId) {
}
