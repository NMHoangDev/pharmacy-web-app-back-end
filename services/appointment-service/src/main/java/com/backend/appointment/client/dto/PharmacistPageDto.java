package com.backend.appointment.client.dto;

import java.util.List;

public record PharmacistPageDto(
        List<PharmacistListItemDto> content,
        int totalPages,
        long totalElements) {
}
