package com.backend.appointment.client.dto.pos;

import java.util.UUID;

public record PosProductSearchItemDto(
        UUID productId,
        String sku,
        String name,
        String imageUrl,
        Long unitPrice,
        Integer available,
        Integer onHand,
        Integer reserved,
        String lotNo,
        String expiryDate) {
}
