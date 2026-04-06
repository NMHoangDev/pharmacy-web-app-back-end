package com.backend.content.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TagCreateRequest(
        @NotBlank String name,
        String slug,
        @NotBlank String type) {
}
