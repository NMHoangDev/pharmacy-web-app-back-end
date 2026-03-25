package com.backend.catalog.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "catalog";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_PRODUCT_LIST = "product-list";
    public static final String TTL_PRODUCT_DETAIL = "product-detail";
    public static final String TTL_CATEGORY_LIST = "category-list";
}
