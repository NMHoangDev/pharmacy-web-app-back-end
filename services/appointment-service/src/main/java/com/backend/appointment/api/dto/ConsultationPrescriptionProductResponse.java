package com.backend.appointment.api.dto;

import java.util.UUID;

public record ConsultationPrescriptionProductResponse(
        UUID productId,
        String name) {
}
