package com.backend.payment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PaymentInitiateRequest(
                @NotBlank String orderId,
                @Min(1) double amount,
                @NotBlank String provider, // VNPAY, ZALOPAY
                String orderInfo,
                String returnUrl) {
}
