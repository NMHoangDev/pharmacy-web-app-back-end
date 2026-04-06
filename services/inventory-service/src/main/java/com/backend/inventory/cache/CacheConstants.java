package com.backend.inventory.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "inventory";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_INVENTORY_LIST = "inventory-list";
    public static final String TTL_INVENTORY_DETAIL = "inventory-detail";
    public static final String TTL_INVENTORY_PRODUCT = "inventory-product";
    public static final String TTL_INVENTORY_ACTIVITY = "inventory-activity";
}
