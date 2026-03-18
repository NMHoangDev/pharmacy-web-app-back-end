package com.backend.order.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        String orderCode,
        UUID userId,
        String status,
        String paymentStatus,
        String paymentMethod,
        double subtotal,
        double shippingFee,
        double discountAmount,
        double totalAmount,
        String note,
        Instant createdAt,
        Instant updatedAt,
        OrderDetailShippingAddressResponse shippingAddress,
        List<OrderDetailItemResponse> items,
        OrderTrackingResponse tracking) {
}
