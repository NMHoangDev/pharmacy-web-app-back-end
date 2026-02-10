package com.backend.appointment.client.dto;

import java.util.UUID;

public record PharmacistPreviewDto(
        UUID id,
        String name,
        String image,
        String specialty) {
}
