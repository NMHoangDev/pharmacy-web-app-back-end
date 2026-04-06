package com.backend.ai.client.dto;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityBatchItem(
        UUID productId,
        List<InventoryAvailabilityByBranch> byBranch) {
}
