package com.backend.branch.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchStatusRequest(@NotBlank String status) {
}
