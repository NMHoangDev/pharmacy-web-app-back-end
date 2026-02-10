package com.backend.order.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
                UUID id,
                UUID userId,
                UUID branchId,
                String status,
                Instant createdAt,
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
                List<OrderResponseItem> items) {
}
