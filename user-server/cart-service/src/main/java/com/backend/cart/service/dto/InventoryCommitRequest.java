package com.backend.cart.service.dto;

import java.util.UUID;

public record InventoryCommitRequest(UUID reservationId, UUID orderId) {
}
