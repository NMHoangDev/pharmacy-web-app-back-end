package com.backend.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveRequest(@NotNull UUID orderId,
                @NotEmpty List<ItemQuantity> items,
                @Min(1) Integer ttlSeconds,
                String reason,
                String actor,
                UUID branchId) {
}
