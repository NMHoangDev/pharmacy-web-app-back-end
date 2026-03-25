package com.backend.content.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
