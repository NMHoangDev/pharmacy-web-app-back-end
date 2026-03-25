package com.backend.common.model;

/**
 * Distributed cache invalidation event payload.
 */
public record CacheInvalidationEvent(String eventType, String entityId, String service) {
}
