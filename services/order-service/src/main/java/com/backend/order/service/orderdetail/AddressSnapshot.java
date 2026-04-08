package com.backend.order.service.orderdetail;

import java.util.UUID;

public record AddressSnapshot(
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
