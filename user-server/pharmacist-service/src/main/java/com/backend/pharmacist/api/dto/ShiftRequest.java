package com.backend.pharmacist.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ShiftRequest(
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt,
        String note) {
}
