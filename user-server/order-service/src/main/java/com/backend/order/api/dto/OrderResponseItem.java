package com.backend.order.api.dto;

import java.util.UUID;

public record OrderResponseItem(UUID productId, String productName, double unitPrice, int quantity) {
}
