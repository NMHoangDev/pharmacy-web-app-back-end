package com.backend.adminbff.dto;

import java.util.UUID;

public record AdminOrderItem(
        UUID productId,
        String productName,
        double unitPrice,
        int quantity) {
}