package com.backend.appointment.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConsultationPrescriptionOrderRequest(
        @NotEmpty List<@Valid ConsultationPrescriptionItemRequest> items,
        @Min(0) Long discount,
        @Min(0) Long taxFee,
        String customerName,
        String customerPhone,
        String note,
        String prescriptionSummary,
        String prescriptionTitle) {
}
