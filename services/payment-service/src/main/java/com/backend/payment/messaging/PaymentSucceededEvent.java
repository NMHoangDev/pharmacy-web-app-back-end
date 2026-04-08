package com.backend.payment.messaging;

import java.time.Instant;

public record PaymentSucceededEvent(
        String type,
        String orderId,
        String provider,
        String txnRef,
        long amount,
        String currency,
        Instant paidAt) {
}
