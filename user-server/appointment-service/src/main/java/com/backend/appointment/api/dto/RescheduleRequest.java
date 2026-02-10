package com.backend.appointment.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull @Future LocalDateTime startAt,
        @NotNull @Future LocalDateTime endAt,
        String reason) {
}