package com.backend.review.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewReplyRequest(@NotBlank String content) {
}
