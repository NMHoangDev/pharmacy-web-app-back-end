package com.backend.pharmacist.api.dto.pos;

import java.time.LocalDate;
import java.util.UUID;

public record OfflineOrderItemResponse(
        UUID id,
        UUID productId,
        String sku,
        String productName,
        String batchNo,
        LocalDate expiryDate,
        Integer qty,
        Long unitPrice,
        Long lineTotal) {
}
