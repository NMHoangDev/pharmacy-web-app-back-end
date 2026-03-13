package com.backend.appointment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConsultationPrescriptionItemRequest(
        @NotNull UUID productId,
        String sku,
        String name,
        String batchNo,
        String expiryDate,
        @Min(1) Integer quantity,
        @Min(0) Long unitPrice,
        String dose,
        String frequency,
        String duration,
        String instruction) {
}
