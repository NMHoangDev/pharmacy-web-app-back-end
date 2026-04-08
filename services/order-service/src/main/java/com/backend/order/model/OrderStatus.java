package com.backend.order.model;

public enum OrderStatus {
    DRAFT,
    PENDING_PAYMENT,
    PLACED,
    CONFIRMED,
    SHIPPING,
    COMPLETED,
    CANCELED
}
