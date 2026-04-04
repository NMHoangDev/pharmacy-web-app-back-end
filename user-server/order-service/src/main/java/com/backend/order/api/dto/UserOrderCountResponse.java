package com.backend.order.api.dto;

import java.util.UUID;

public record UserOrderCountResponse(
        UUID userId,
        long orderCount) {
}
