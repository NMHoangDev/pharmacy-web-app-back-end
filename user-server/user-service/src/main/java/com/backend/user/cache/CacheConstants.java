package com.backend.user.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "user";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_USER_LIST = "user-list";
    public static final String TTL_USER_DETAIL = "user-detail";
}
