package com.backend.order.service.orderdetail;

public record ProductSnapshot(
        String name,
        String imageUrl,
        String sku,
        String unit,
        String category,
        String type,
        String shortDescription) {
}
