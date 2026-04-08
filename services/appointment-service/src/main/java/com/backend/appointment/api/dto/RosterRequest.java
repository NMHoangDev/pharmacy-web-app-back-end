package com.backend.appointment.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public record RosterRequest(
                @NotNull UUID pharmacistId,
                UUID branchId,
                @Min(1) @Max(7) int dayOfWeek,
                @NotNull LocalTime startTime,
                @NotNull LocalTime endTime) {
}