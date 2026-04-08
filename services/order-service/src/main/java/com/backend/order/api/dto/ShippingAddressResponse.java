package com.backend.order.api.dto;

public record ShippingAddressResponse(
        String fullName,
        String phone,
        String addressLine,
        String provinceName,
        String provinceCode,
        String districtName,
        String districtCode,
        String wardName,
        String wardCode) {
}
