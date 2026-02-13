package com.backend.inventory.api.dto;

import java.util.UUID;

public record ReleaseRequest(UUID reservationId, UUID orderId, String reason, String actor, UUID branchId) {
}
