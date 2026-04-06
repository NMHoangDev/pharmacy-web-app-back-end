package com.backend.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminRoleAssignmentRequest(@NotBlank String role) {
}
