package com.backend.user.api.dto;

import java.util.UUID;

public record AddressResponse(UUID id,
        String label,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        Boolean isDefault) {
}
