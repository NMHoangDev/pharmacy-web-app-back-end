package com.backend.appointment.cache;

public final class CacheConstants {
    private CacheConstants() {
    }

    public static final String SERVICE_NAME = "appointment";
    public static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation-events";

    public static final String TTL_APPOINTMENT_LIST = "appointment-list";
    public static final String TTL_APPOINTMENT_DETAIL = "appointment-detail";
    public static final String TTL_APPOINTMENT_USER = "appointment-user";
    public static final String TTL_APPOINTMENT_PHARMACIST = "appointment-pharmacist";
}
