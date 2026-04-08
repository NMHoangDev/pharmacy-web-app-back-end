package com.backend.review.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
