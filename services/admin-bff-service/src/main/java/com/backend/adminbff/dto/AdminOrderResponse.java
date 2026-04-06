package com.backend.adminbff.dto;

import java.util.List;
import java.util.UUID;

public record AdminOrderResponse(
        UUID id,
        UUID userId,
        String status,
        String createdAt,
        double subtotal,
        double shippingFee,
        double discountAmount,
        double totalAmount,
        String paymentMethod,
        String paymentStatus,
        String note,
        ShippingAddressResponse shippingAddress,
        ShippingResponse shipping,
        DiscountResponse discount,
        List<AdminOrderItem> items) {
}