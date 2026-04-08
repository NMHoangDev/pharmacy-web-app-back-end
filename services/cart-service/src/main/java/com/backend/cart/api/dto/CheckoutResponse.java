package com.backend.cart.api.dto;

import java.util.UUID;

public record CheckoutResponse(UUID orderId, String status, UUID reservationId) {
}
