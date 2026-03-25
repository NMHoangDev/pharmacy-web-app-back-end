package com.backend.common.messaging;

/**
 * Central place to keep topic names consistent.
 */
public final class TopicNames {
    private TopicNames() {
    }

    public static final String ORDER_EVENTS = "order.events";

    // User-facing notification topics
    public static final String DISCOUNT_NOTIFICATION = "discount-notification";
    public static final String INVENTORY_EVENTS = "inventory.events";
    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String APPOINTMENT_EVENTS = "appointment.events";
    public static final String REVIEW_EVENTS = "review.events";
    public static final String CMS_EVENTS = "cms.events";
    public static final String USER_EVENTS = "user.events";
    public static final String CACHE_INVALIDATION_EVENTS = "cache.invalidation.events";
}
