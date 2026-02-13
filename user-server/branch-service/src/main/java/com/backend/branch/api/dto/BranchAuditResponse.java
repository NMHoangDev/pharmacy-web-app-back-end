package com.backend.branch.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BranchAuditResponse(
        UUID id,
        UUID branchId,
        String actor,
        String action,
        String entity,
        String beforeJson,
        String afterJson,
        Instant createdAt) {
}
