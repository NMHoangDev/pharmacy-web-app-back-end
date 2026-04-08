package com.backend.payment.messaging;

public record PaymentStatusEvent(
        String orderId,
        String status, // PAID, FAILED
        String provider,
        String transactionRef) {
}
