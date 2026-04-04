package com.backend.ai.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChatSourceDto(
        String type,
        UUID id,
        String title,
        String snippet,
        String stockStatus,
        Integer available,
        BigDecimal price,
        String source,
        String topic,
        String audience,
        String riskLevel,
        String warning) {
}
