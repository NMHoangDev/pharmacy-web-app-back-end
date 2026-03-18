package com.backend.common.messaging;

/**
 * Event type identifiers to avoid magic strings across services.
 */
public final class EventTypes {
    private EventTypes() {
    }

    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_STATUS_UPDATED = "order.status.updated";
    public static final String CART_ITEM_ADDED = "cart.item.added";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_RELEASED = "stock.released";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String APPOINTMENT_CONFIRMED = "appointment.confirmed";
    public static final String APPOINTMENT_CREATED = "appointment.created";
    public static final String APPOINTMENT_ACCEPTED = "appointment.accepted";
    public static final String APPOINTMENT_REJECTED = "appointment.rejected";
    public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String APPOINTMENT_STATUS_UPDATED = "appointment.status.updated";
    public static final String REVIEW_CREATED = "review.created";
    public static final String CMS_PUBLISHED = "cms.published";
    public static final String USER_CREATED = "user.created";
}
