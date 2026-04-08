package com.backend.branch.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BranchStaffRequest(
        @NotNull UUID userId,
        @NotBlank String role,
        String skillsJson,
        Boolean active) {
}
