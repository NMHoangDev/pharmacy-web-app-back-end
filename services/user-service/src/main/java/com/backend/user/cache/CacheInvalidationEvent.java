package com.backend.user.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
