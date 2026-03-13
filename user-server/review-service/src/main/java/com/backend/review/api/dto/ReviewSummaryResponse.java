package com.backend.review.api.dto;

import java.util.Map;
import java.util.UUID;

public record ReviewSummaryResponse(UUID productId, double averageRating, long totalReviews,
        Map<Integer, Long> ratingCounts) {
}