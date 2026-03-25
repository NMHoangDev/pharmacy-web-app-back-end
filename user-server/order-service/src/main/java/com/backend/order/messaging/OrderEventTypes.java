package com.backend.order.messaging;

public final class OrderEventTypes {

    public static final String CART_ITEM_ADDED = "CART_ITEM_ADDED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_UPDATED = "ORDER_STATUS_UPDATED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";

    private OrderEventTypes() {
    }
}
