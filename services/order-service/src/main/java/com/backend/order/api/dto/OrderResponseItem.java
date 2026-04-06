package com.backend.order.api.dto;

import java.util.UUID;

public record OrderResponseItem(
        UUID productId,
        String productName,
        String imageUrl,
        String sku,
        String unit,
        String category,
        String type,
        String shortDescription,
        double unitPrice,
        int quantity) {
}
