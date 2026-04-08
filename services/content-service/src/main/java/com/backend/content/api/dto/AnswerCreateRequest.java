package com.backend.content.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AnswerCreateRequest(
        @NotBlank String content,
        List<AnswerReference> references) {
}
