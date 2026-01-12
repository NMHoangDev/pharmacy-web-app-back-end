package com.backend.common.messaging;

/**
 * Standard Kafka header keys for domain events.
 */
public final class KafkaHeadersEx {
    private KafkaHeadersEx() {
    }

    public static final String EVENT_TYPE = "event-type";
    public static final String EVENT_VERSION = "event-version";
    public static final String EVENT_ID = "event-id";
    public static final String CORRELATION_ID = "correlation-id";
    public static final String TENANT = "tenant";
    public static final String ACTOR = "actor";
}
