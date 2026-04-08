package com.backend.payment.messaging;

public record PaymentFailedEvent(
        String type,
        String orderId,
        String provider,
        String txnRef,
        long amount,
        String currency,
        String reasonCode) {
}
