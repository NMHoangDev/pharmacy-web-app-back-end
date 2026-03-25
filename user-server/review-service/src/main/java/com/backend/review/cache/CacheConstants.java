package com.backend.review.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "review";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_REVIEW = "review-list";
    public static final String TTL_REVIEW_DETAIL = "review-detail";
}
