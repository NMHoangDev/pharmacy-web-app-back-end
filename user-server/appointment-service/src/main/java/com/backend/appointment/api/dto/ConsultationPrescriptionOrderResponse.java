package com.backend.appointment.api.dto;

import java.util.UUID;

public record ConsultationPrescriptionOrderResponse(
        UUID orderId,
        String orderCode,
        String consultationId,
        Long totalAmount,
        String status) {
}
