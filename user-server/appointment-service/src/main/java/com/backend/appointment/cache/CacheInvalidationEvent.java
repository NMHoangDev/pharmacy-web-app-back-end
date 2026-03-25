package com.backend.appointment.cache;

public record CacheInvalidationEvent(String entity, String type, String id) {
}
