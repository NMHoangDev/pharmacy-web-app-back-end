package com.backend.order.api.dto;

import java.util.List;
import java.util.UUID;

public record BranchAvailabilityItem(
        UUID productId,
        String name,
        int quantityOrdered,
        List<BranchStockAvailability> byBranch) {
}
