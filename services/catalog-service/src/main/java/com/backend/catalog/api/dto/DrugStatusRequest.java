package com.backend.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DrugStatusRequest(@NotBlank String status) {
}
