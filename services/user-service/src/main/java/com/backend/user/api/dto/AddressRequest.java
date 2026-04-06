package com.backend.user.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,
        @NotBlank String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isDefault) {
}
