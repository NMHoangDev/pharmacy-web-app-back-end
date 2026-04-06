package com.backend.order.api.dto;

import java.util.UUID;

public record OrderDetailShippingAddressResponse(
        UUID id,
        String recipientName,
        String phoneNumber,
        String province,
        String city,
        String district,
        String ward,
        String streetAddress,
        String detailAddress,
        String fullAddress,
        String addressType,
        Boolean isDefault) {
}
