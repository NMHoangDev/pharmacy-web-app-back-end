package com.backend.inventory.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
