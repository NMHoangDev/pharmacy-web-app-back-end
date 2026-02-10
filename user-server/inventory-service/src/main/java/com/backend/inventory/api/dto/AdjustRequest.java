package com.backend.inventory.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AdjustRequest(@NotNull UUID productId,
                @NotNull Integer delta,
                String reason,
                String actor,
                String refType,
                UUID refId,
                UUID branchId,
                String batchNo,
                LocalDate expiryDate) {
}
