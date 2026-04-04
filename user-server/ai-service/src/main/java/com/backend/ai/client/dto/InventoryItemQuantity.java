package com.backend.ai.client.dto;

import java.util.UUID;

public record InventoryItemQuantity(UUID productId, int qty) {
}
