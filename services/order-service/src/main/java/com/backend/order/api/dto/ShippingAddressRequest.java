package com.backend.order.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotBlank String addressLine,
        @NotBlank String provinceName,
        @NotBlank String provinceCode,
        @NotBlank String districtName,
        @NotBlank String districtCode,
        @NotBlank String wardName,
        @NotBlank String wardCode) {
}
