package com.backend.payment.api.dto;

public record PaymentInitiateResponse(
        String paymentUrl,
        String message) {
}
