package com.pharmacy.discount.kafka;

public final class DiscountEventTypes {
    private DiscountEventTypes() {
    }

    public static final String DISCOUNT_CREATED = "DISCOUNT_CREATED";
    public static final String DISCOUNT_UPDATED = "DISCOUNT_UPDATED";
    public static final String DISCOUNT_USED = "DISCOUNT_USED";
}
