package com.backend.branch.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BranchHoursRequest(
        @NotBlank String weeklyHoursJson,
        String lunchBreakJson) {
}
