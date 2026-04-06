package com.backend.ai.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatTurnRequest(
        @NotBlank String role,
        @NotBlank String content) {
}
