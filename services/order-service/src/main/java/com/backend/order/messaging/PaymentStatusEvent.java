package com.backend.order.messaging;

public record PaymentStatusEvent(
        String orderId,
        String status, // PAID, FAILED
        String provider,
        String transactionRef) {
}
