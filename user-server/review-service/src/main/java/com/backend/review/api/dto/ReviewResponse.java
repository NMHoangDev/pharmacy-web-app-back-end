package com.backend.review.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(UUID id,
        UUID productId,
        UUID userId,
        Integer rating,
        String title,
        String content,
        String replyContent,
        String status,
        Instant createdAt,
        Instant updatedAt,
        Instant repliedAt,
        List<ReviewImageResponse> images) {
}
