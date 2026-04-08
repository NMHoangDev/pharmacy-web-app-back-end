package com.backend.appointment.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRequest(
        @NotNull UUID pharmacistId,
        String reason) {
}