package com.backend.adminbff.dto;

import java.util.UUID;

public record UserOrderCountResponse(
        UUID userId,
        long orderCount) {
}
