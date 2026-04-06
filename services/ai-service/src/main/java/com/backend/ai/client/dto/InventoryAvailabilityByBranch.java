package com.backend.ai.client.dto;

import java.util.UUID;

public record InventoryAvailabilityByBranch(
        UUID branchId,
        int available,
        int onHand,
        int reserved) {
}
