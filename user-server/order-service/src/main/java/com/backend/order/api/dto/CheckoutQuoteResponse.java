package com.backend.order.api.dto;

public record CheckoutQuoteResponse(
        double subtotal,
        double shippingFee,
        double discountAmount,
        double grandTotal) {
}
