package com.backend.branch.api.dto;

import java.util.UUID;

public record BranchPrimaryStaffRequest(
        UUID branchId,
        String role,
        String skillsJson,
        Boolean active) {
}
