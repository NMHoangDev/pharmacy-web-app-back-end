package com.backend.order.api.dto;

import java.util.UUID;

public record OrderDetailItemResponse(
        UUID productId,
        String productName,
        String imageUrl,
        String sku,
        String unit,
        double price,
        int quantity,
        double lineTotal,
        String category,
        String type,
        String shortDescription) {
}
