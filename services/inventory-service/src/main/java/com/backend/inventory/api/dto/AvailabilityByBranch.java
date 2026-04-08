package com.backend.inventory.api.dto;

import java.util.UUID;

public record AvailabilityByBranch(UUID branchId, int available, int onHand, int reserved) {
}
