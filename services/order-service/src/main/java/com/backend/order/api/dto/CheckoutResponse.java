package com.backend.order.api.dto;

import java.util.UUID;

public record CheckoutResponse(UUID orderId, String status) {
}
