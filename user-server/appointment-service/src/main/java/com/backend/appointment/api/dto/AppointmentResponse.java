package com.backend.appointment.api.dto;

import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.model.Channel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(UUID id,
        UUID userId,
        UUID pharmacistId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        AppointmentStatus status,
        Channel channel,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
