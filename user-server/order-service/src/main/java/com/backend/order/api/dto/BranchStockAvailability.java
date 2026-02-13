package com.backend.order.api.dto;

import java.util.UUID;

public record BranchStockAvailability(
                UUID branchId,
                int availableQty,
                int onHandQty,
                int reservedQty) {
}
