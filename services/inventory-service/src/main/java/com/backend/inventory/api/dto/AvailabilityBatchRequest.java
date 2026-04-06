package com.backend.inventory.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AvailabilityBatchRequest(
                @NotEmpty List<UUID> branchIds,
                @NotEmpty List<ItemQuantity> items) {
}
