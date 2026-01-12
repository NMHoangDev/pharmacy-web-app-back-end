package com.backend.inventory.api.dto;

import java.util.UUID;

public record CommitRequest(UUID reservationId, UUID orderId) {
}
