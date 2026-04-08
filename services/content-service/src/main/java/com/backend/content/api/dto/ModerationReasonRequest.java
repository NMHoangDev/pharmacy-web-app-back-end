package com.backend.content.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ModerationReasonRequest(@NotBlank String reason) {
}
