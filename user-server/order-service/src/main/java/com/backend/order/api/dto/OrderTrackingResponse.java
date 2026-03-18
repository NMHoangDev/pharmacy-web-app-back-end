package com.backend.order.api.dto;

import java.util.List;

public record OrderTrackingResponse(
        String currentStatus,
        List<OrderStatusHistoryResponse> statusHistory) {
}
