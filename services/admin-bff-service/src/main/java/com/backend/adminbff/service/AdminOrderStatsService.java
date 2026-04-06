package com.backend.adminbff.service;

import com.backend.adminbff.dto.AdminOrderResponse;
import com.backend.adminbff.dto.stats.OrdersStatsResponse;
import com.backend.adminbff.repository.AdminOrderStatsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderStatsService {

    private final AdminOrderStatsRepository repository;

    public AdminOrderStatsService(AdminOrderStatsRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "admin-orders-stats", key = "#range")
    public OrdersStatsResponse getStats(String range) {
        List<AdminOrderResponse> orders = repository.findAllOrders();

        long total = orders.size();
        long waitingConfirmation = orders.stream()
                .filter(o -> isAnyStatus(o.status(), "DRAFT", "PENDING_PAYMENT", "PLACED"))
                .count();
        long processing = orders.stream()
                .filter(o -> isAnyStatus(o.status(), "CONFIRMED"))
                .count();
        long shipping = orders.stream().filter(o -> isAnyStatus(o.status(), "SHIPPING")).count();
        long completed = orders.stream().filter(o -> isAnyStatus(o.status(), "COMPLETED")).count();
        long cancelled = orders.stream().filter(o -> isAnyStatus(o.status(), "CANCELED", "CANCELLED")).count();

        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate weekStart = today.minusDays(6);

        BigDecimal revenueToday = orders.stream()
                .filter(o -> isAnyStatus(o.status(), "SHIPPING", "COMPLETED"))
                .filter(o -> isSameDate(o.createdAt(), today, zoneId))
                .map(o -> BigDecimal.valueOf(o.totalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal revenueThisWeek = orders.stream()
                .filter(o -> isAnyStatus(o.status(), "SHIPPING", "COMPLETED"))
                .filter(o -> inDateRange(o.createdAt(), weekStart, today, zoneId))
                .map(o -> BigDecimal.valueOf(o.totalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new OrdersStatsResponse(
                total,
                waitingConfirmation,
                processing,
                shipping,
                completed,
                cancelled,
                revenueToday,
                revenueThisWeek);
    }

    private boolean isAnyStatus(String status, String... candidates) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (normalized.equals(candidate.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameDate(String createdAt, LocalDate target, ZoneId zoneId) {
        LocalDate date = parseDate(createdAt, zoneId);
        return date != null && date.isEqual(target);
    }

    private boolean inDateRange(String createdAt, LocalDate from, LocalDate to, ZoneId zoneId) {
        LocalDate date = parseDate(createdAt, zoneId);
        return date != null && !date.isBefore(from) && !date.isAfter(to);
    }

    private LocalDate parseDate(String createdAt, ZoneId zoneId) {
        if (createdAt == null || createdAt.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(createdAt).atZone(zoneId).toLocalDate();
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(createdAt).atZoneSameInstant(zoneId).toLocalDate();
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }
}
