package com.backend.payment.api.dto;

public record PaymentMethodResponse(
        String id,
        String name,
        String icon) {
}
