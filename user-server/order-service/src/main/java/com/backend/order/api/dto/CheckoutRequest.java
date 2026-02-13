package com.backend.order.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotNull UUID userId,
        @NotEmpty List<CheckoutItem> items,
        UUID branchId,
        ShippingAddressRequest shippingAddress,
        String shippingMethod,
        String paymentMethod,
        String promoCode,
        String note) {
}
