package com.backend.order.api.dto;

import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID id, UUID userId, String status, double totalAmount, List<OrderResponseItem> items) {
}
