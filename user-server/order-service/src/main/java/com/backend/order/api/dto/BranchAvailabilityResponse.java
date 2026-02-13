package com.backend.order.api.dto;

import java.util.List;
import java.util.UUID;

public record BranchAvailabilityResponse(
                UUID orderId,
                List<BranchAvailabilityItem> items,
                List<BranchSummaryDto> branches,
                List<UUID> recommendedBranches) {
}
