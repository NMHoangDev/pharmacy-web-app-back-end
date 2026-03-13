package com.backend.payment.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record ZaloPayCreateRequest(
        @NotBlank String orderId,
        @Min(1) long amount,
        @NotBlank String description,
        List<Map<String, Object>> items,
        Map<String, Object> embedData) {
}
