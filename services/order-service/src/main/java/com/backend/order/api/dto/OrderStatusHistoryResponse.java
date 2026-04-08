package com.backend.order.api.dto;

import java.time.Instant;

public record OrderStatusHistoryResponse(
        String status,
        Instant changedAt,
        String note) {
}
