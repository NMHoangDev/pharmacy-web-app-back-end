package com.backend.adminbff.dto;

public record ShippingResponse(
        String method,
        double fee,
        String etaRange) {
}