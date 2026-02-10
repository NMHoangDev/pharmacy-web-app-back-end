package com.backend.appointment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BreakGlassRequest(@NotBlank String reason) {
}