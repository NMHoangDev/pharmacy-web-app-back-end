package com.backend.consultation.client.dto;

import java.util.UUID;

public record PharmacistDto(UUID id, String name, String status) {
}
