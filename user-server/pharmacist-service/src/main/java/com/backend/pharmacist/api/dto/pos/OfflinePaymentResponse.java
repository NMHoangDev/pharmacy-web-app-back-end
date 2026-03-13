package com.backend.pharmacist.api.dto.pos;

import com.backend.pharmacist.model.OfflinePaymentEventType;
import com.backend.pharmacist.model.OfflinePaymentMethod;

import java.time.Instant;
import java.util.UUID;

public record OfflinePaymentResponse(
        UUID id,
        OfflinePaymentEventType eventType,
        OfflinePaymentMethod method,
        Long amount,
        Long amountReceived,
        Long changeAmount,
        String transferReference,
        String proofUrl,
        String note,
        UUID createdBy,
        Instant createdAt) {
}
