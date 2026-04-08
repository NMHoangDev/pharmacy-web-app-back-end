package com.backend.order.api.dto;

public record ShippingResponse(
        String method,
        double fee,
        String etaRange) {
}
