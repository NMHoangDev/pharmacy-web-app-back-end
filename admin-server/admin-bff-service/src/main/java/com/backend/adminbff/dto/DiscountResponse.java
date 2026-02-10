package com.backend.adminbff.dto;

public record DiscountResponse(
        String promoCode,
        double discountAmount,
        String description) {
}