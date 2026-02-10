package com.backend.branch.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BranchStaffResponse(
        UUID branchId,
        UUID userId,
        String role,
        String skillsJson,
        boolean active,
        Instant createdAt) {
}
