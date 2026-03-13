package com.backend.payment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record VnpayCreateRequest(
        @NotBlank String orderId,
        @Min(1) long amount,
        @NotBlank String orderInfo,
        String orderType,
        String bankCode,
        String locale) {
}
