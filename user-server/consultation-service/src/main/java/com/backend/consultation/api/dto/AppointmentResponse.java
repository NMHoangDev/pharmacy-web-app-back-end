package com.backend.consultation.api.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID pharmacistId,
        UUID userId,
        String fullName,
        String contact,
        String mode,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String note,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
