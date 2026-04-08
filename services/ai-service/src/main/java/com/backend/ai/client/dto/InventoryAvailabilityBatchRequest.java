package com.backend.ai.client.dto;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityBatchRequest(
        List<UUID> branchIds,
        List<InventoryItemQuantity> items) {
}
