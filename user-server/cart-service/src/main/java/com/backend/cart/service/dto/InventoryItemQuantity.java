package com.backend.cart.service.dto;

import java.util.UUID;

public record InventoryItemQuantity(UUID productId, int qty) {
}
