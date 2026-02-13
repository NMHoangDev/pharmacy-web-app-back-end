package com.backend.order.api.dto;

public record ShippingMethodResponse(
        String id,
        String name,
        String description,
        double fee,
        String feeLabel) {
}
