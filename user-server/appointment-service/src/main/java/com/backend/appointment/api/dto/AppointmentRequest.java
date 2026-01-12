package com.backend.appointment.api.dto;

import com.backend.appointment.model.Channel;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull UUID userId,
        @NotNull UUID pharmacistId,
        @NotNull @Future LocalDateTime startAt,
        @NotNull @Future LocalDateTime endAt,
        Channel channel,
        String notes) {
}
