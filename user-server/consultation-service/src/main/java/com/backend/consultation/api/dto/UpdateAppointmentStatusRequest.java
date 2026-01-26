package com.backend.consultation.api.dto;

public record UpdateAppointmentStatusRequest(
        String status,
        String cancelReason) {
}
