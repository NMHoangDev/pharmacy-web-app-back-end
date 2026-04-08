package com.backend.branch.event;

import java.time.Instant;
import java.util.UUID;

public record BranchEventPayload(
        UUID branchId,
        String code,
        String name,
        String status,
        Instant updatedAt) {
}
