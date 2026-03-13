package com.backend.appointment.client.dto;

import java.util.UUID;

public record PharmacistListItemDto(
        UUID id,
        String email,
        String name) {
}
