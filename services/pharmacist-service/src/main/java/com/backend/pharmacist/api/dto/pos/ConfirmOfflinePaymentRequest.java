package com.backend.pharmacist.api.dto.pos;

import com.backend.pharmacist.model.OfflinePaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmOfflinePaymentRequest(
        @NotNull UUID pharmacistId,
        @NotNull OfflinePaymentMethod method,
        @NotNull @Min(0) Long amountReceived,
        String transferReference,
        String paymentProofUrl,
        String note) {
}
