package com.backend.consultation.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        UUID pharmacistId,
        UUID userId,
        String fullName,
        String contact,
        String mode,
        LocalDateTime startAt,
        Integer durationMinutes,
        String note) {
}
