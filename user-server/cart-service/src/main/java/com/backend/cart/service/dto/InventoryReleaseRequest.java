package com.backend.cart.service.dto;

import java.util.UUID;

public record InventoryReleaseRequest(UUID reservationId, UUID orderId) {
}
