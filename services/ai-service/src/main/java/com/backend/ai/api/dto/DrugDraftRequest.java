package com.backend.ai.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DrugDraftRequest(@NotBlank String name) {
}
