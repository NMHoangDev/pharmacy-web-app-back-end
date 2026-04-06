package com.backend.pharmacist.api.dto.pos;

import java.util.List;

public record OfflineOrderPageResponse(
        List<OfflineOrderResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages) {
}
