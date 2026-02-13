package com.backend.content.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record QuestionCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        Boolean isAnonymous,
        List<String> tags,
        QuestionContext context) {
}
