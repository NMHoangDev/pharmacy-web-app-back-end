package com.backend.inventory.api.dto;

import java.util.List;

public record StockDocumentPageResponse(
        List<StockDocumentSummaryResponse> items,
        int page,
        int size,
        long total) {
}
