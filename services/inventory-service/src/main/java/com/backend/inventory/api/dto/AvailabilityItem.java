package com.backend.inventory.api.dto;

import java.util.UUID;

public record AvailabilityItem(UUID productId, int available, int onHand, int reserved) {
}
