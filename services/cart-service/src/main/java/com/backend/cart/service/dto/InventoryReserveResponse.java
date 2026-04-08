package com.backend.cart.service.dto;

import java.util.UUID;

public record InventoryReserveResponse(UUID reservationId, String status) {
}
