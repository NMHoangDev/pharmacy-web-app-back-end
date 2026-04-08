package com.backend.review.api.dto;

public record ReviewAdminStatsResponse(long total,
        double average,
        long pending,
        long hidden,
        long responded) {
}
