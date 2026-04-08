package com.backend.order.api.dto;

public record DiscountResponse(
        String promoCode,
        double discountAmount,
        String description) {
}
