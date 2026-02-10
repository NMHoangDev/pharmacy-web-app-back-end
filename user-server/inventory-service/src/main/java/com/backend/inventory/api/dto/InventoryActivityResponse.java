package com.backend.inventory.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InventoryActivityResponse(
                UUID id,
                UUID productId,
                String type,
                int delta,
                int onHandAfter,
                int reservedAfter,
                String reason,
                String refType,
                UUID refId,
                String actor,
                UUID branchId,
                String batchNo,
                LocalDate expiryDate,
                Instant createdAt) {
}
