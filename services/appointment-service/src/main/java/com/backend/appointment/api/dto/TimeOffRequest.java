package com.backend.appointment.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimeOffRequest(
                @NotNull UUID pharmacistId,
                UUID branchId,
                @NotNull @Future LocalDateTime startAt,
                @NotNull @Future LocalDateTime endAt,
                String reason) {
}