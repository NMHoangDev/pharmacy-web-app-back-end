package com.backend.pharmacist.api.dto.pos;

import java.util.UUID;

public record PosProductSearchResponse(
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
