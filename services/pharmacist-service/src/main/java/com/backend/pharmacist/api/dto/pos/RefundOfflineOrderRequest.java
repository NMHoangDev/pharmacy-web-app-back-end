package com.backend.pharmacist.api.dto.pos;

import com.backend.pharmacist.model.OfflinePaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RefundOfflineOrderRequest(
        @NotNull UUID pharmacistId,
        OfflinePaymentMethod method,
        Boolean restock,
        String transferReference,
        String paymentProofUrl,
        String note) {
}
