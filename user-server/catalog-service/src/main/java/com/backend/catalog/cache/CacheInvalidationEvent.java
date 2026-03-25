package com.backend.catalog.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
