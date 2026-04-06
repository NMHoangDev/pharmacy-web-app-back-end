package com.backend.inventory.api.dto;

import java.util.UUID;

public record AdjustResponse(UUID productId, int onHand, int reserved) {
}
