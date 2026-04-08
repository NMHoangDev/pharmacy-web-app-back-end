package com.backend.appointment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment request for booking.
 */
public record AppointmentRequest(
                // optional when booking as guest; if provided ties appointment to user
                UUID userId,
                @NotNull UUID pharmacistId,
                UUID branchId,
                @NotNull @Future LocalDateTime startAt,
                @NotNull @Future LocalDateTime endAt,
                // optional channel and notes
                com.backend.appointment.model.Channel channel,
                String notes) {
}
