package com.backend.common.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic envelope for domain events.
 */
public record EventEnvelope<T>(String id, String type, String version, Instant occurredAt, T payload,
        Map<String, String> metadata) {
    public EventEnvelope {
        id = id == null ? UUID.randomUUID().toString() : id;
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static <T> EventEnvelope<T> of(String type, String version, T payload) {
        return new EventEnvelope<>(null, type, version, null, payload, Map.of());
    }
}
