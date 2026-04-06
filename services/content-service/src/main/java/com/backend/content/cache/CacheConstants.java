package com.backend.content.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "content";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_POST_LIST = "post-list";
    public static final String TTL_POST_DETAIL = "post-detail";
}
