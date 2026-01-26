package com.backend.cart.service.dto;

import java.util.List;
import java.util.UUID;

public record InventoryReserveRequest(UUID orderId, List<InventoryItemQuantity> items, Integer ttlSeconds) {
}
