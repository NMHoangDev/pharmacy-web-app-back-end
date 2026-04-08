package com.backend.inventory.api.dto;

import java.util.List;
import java.util.UUID;

public record AvailabilityBatchItem(UUID productId, List<AvailabilityByBranch> byBranch) {
}
