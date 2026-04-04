package com.backend.adminbff.dto.stats;

import java.math.BigDecimal;

public record OrdersStatsResponse(
        long totalOrders,
        long waitingConfirmationOrders,
        long processingOrders,
        long shippingOrders,
        long completedOrders,
        long cancelledOrders,
        BigDecimal revenueToday,
        BigDecimal revenueThisWeek) {
}
