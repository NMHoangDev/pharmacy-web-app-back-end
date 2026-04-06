package com.backend.notification.messaging;

public final class NotificationEventTypes {

    public static final String CART_ITEM_ADDED = "CART_ITEM_ADDED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_UPDATED = "ORDER_STATUS_UPDATED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
    public static final String APPOINTMENT_ACCEPTED = "APPOINTMENT_ACCEPTED";
    public static final String APPOINTMENT_CONFIRMED = "APPOINTMENT_CONFIRMED";
    public static final String APPOINTMENT_REJECTED = "APPOINTMENT_REJECTED";
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";

    public static final String CMS_PUBLISHED = "CMS_PUBLISHED";

    private NotificationEventTypes() {
    }
}
