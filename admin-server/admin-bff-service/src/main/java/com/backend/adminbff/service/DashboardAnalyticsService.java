package com.backend.adminbff.service;

import com.backend.adminbff.client.AdminCatalogInventoryClient;
import com.backend.adminbff.client.AdminOrderClient;
import com.backend.adminbff.client.AdminPosClient;
import com.backend.adminbff.client.AdminUserClient;
import com.backend.adminbff.dto.AdminOrderItem;
import com.backend.adminbff.dto.AdminOrderResponse;
import com.backend.adminbff.dto.AdminUserProfile;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DashboardAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardAnalyticsService.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final AdminOrderClient adminOrderClient;
    private final AdminPosClient adminPosClient;
    private final AdminUserClient adminUserClient;
    private final AdminCatalogInventoryClient catalogInventoryClient;
    private final ObjectMapper objectMapper;

    public DashboardAnalyticsService(
            AdminOrderClient adminOrderClient,
            AdminPosClient adminPosClient,
            AdminUserClient adminUserClient,
            AdminCatalogInventoryClient catalogInventoryClient,
            ObjectMapper objectMapper) {
        this.adminOrderClient = adminOrderClient;
        this.adminPosClient = adminPosClient;
        this.adminUserClient = adminUserClient;
        this.catalogInventoryClient = catalogInventoryClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getAnalytics(String requestedRange, UUID branchId) {
        String range = normalizeRange(requestedRange);

        DateRange current = currentRange(range);
        DateRange previous = previousRange(range, current);

        List<AdminOrderResponse> onlineOrders = safeFetch(
                () -> adminOrderClient.listOrders(null, null),
                "online orders",
                List.of());
        List<Map<String, Object>> posOrders = safeFetch(
                () -> adminPosClient.listAllPosOrders(branchId),
                "pos orders",
                List.of());
        List<AdminUserProfile> users = safeFetch(adminUserClient::listUsers, "users", List.of());

        List<AdminOrderResponse> onlineRange = onlineOrders.stream()
                .filter(o -> isInRange(parseInstant(o.createdAt()), current))
                .filter(o -> isOnlineRevenueStatus(o.status()))
                .toList();
        List<AdminOrderResponse> onlinePrev = onlineOrders.stream()
                .filter(o -> isInRange(parseInstant(o.createdAt()), previous))
                .filter(o -> isOnlineRevenueStatus(o.status()))
                .toList();

        List<Map<String, Object>> posRange = posOrders.stream()
                .filter(o -> isInRange(parseInstant(asString(o.get("createdAt"))), current))
                .filter(o -> isPosRevenueStatus(asString(o.get("status"))))
                .toList();
        List<Map<String, Object>> posPrev = posOrders.stream()
                .filter(o -> isInRange(parseInstant(asString(o.get("createdAt"))), previous))
                .filter(o -> isPosRevenueStatus(asString(o.get("status"))))
                .toList();

        long onlineRevenue = onlineRange.stream().mapToLong(o -> toLong(o.totalAmount())).sum();
        long posRevenue = posRange.stream().mapToLong(o -> toLong(o.get("totalAmount"))).sum();
        long totalRevenue = onlineRevenue + posRevenue;

        long revenuePrev = onlinePrev.stream().mapToLong(o -> toLong(o.totalAmount())).sum()
                + posPrev.stream().mapToLong(o -> toLong(o.get("totalAmount"))).sum();

        int onlineOrdersCount = onlineRange.size();
        int onlineOrdersCountPrev = onlinePrev.size();

        int onlineSoldQty = onlineRange.stream()
                .mapToInt(o -> o.items() == null ? 0 : o.items().stream().mapToInt(this::adminItemQty).sum())
                .sum();

        int posSoldQty = posRange.stream().mapToInt(this::sumPosItemsQty).sum();
        int totalSoldQty = onlineSoldQty + posSoldQty;

        int pendingOrders = (int) onlineOrders.stream().filter(o -> isPendingOnline(o.status())).count();

        Double revenueChange = pctChange(totalRevenue, revenuePrev);
        Double ordersChange = pctChange(onlineOrdersCount, onlineOrdersCountPrev);

        InventoryComputed inventoryComputed = computeInventory(branchId);

        List<Map<String, Object>> bars = buildRevenueBars(range, onlineRange, posRange);
        List<Map<String, Object>> topProducts = buildTopProducts(onlineRange, posRange);
        List<Map<String, Object>> recentOrders = buildRecentOnlineOrders(onlineRange);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRevenue", totalRevenue);
        summary.put("posRevenue", posRevenue);
        summary.put("onlineRevenue", onlineRevenue);
        summary.put("revenuePrev", revenuePrev);
        summary.put("revenueChange", revenueChange);
        summary.put("onlineOrdersCount", onlineOrdersCount);
        summary.put("onlineOrdersCountPrev", onlineOrdersCountPrev);
        summary.put("ordersChange", ordersChange);
        summary.put("onlineSoldQty", onlineSoldQty);
        summary.put("posSoldQty", posSoldQty);
        summary.put("totalSoldQty", totalSoldQty);
        summary.put("usersCount", users.size());
        summary.put("pendingOrders", pendingOrders);
        summary.put("appointmentsToday", 0);
        summary.put("inventorySummary", inventoryComputed.inventorySummary);
        summary.put("lowStockCount", inventoryComputed.lowStockItems.size());

        Map<String, Object> revenueChart = Map.of(
                "title", "Biểu đồ tổng doanh thu",
                "subtitle", "Gộp doanh thu online (order-service) và doanh thu tại quầy (pharmacist_pos)",
                "bars", bars);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("range", range);
        response.put("branchId", branchId);
        response.put("generatedAt", Instant.now().toString());
        response.put("summary", summary);
        response.put("revenueChart", revenueChart);
        response.put("topProducts", topProducts);
        response.put("recentOrders", recentOrders);
        response.put("lowStockItems", inventoryComputed.lowStockItems);

        return response;
    }

    private InventoryComputed computeInventory(UUID branchId) {
        List<Map<String, Object>> products = safeFetch(
                () -> catalogInventoryClient.listCatalogProducts(branchId),
                "catalog products",
                List.of());

        List<UUID> productIds = products.stream()
                .map(p -> asUuid(p.get("id")))
                .filter(id -> id != null)
                .toList();

        List<Map<String, Object>> availabilityRows = safeFetch(
                () -> catalogInventoryClient.getInventoryAvailability(productIds, branchId),
                "inventory availability",
                List.of());

        Map<String, Map<String, Object>> availabilityByProduct = availabilityRows.stream()
                .filter(row -> row.get("productId") != null)
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("productId")),
                        row -> row,
                        (a, b) -> a));

        long onHandTotal = 0;
        long availableTotal = 0;
        int outOfStock = 0;

        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        for (Map<String, Object> product : products) {
            UUID productId = asUuid(product.get("id"));
            if (productId == null) {
                continue;
            }
            Map<String, Object> inv = availabilityByProduct.get(String.valueOf(productId));

            int onHand = toInt(inv != null ? inv.get("onHand") : 0);
            int reserved = toInt(inv != null ? inv.get("reserved") : 0);
            int available = inv != null && inv.get("available") != null
                    ? toInt(inv.get("available"))
                    : Math.max(onHand - reserved, 0);

            onHandTotal += onHand;
            availableTotal += available;
            if (available <= 0) {
                outOfStock += 1;
            }

            int threshold = resolveThreshold(product);
            if (onHand <= threshold) {
                lowStockItems.add(Map.of(
                        "id", productId,
                        "name", asString(product.get("name"), "Sản phẩm"),
                        "sku", asString(product.get("sku"), ""),
                        "onHand", onHand,
                        "reserved", reserved,
                        "available", available,
                        "threshold", threshold));
            }
        }

        lowStockItems.sort(Comparator.comparingInt(row -> toInt(row.get("onHand"))));
        if (lowStockItems.size() > 6) {
            lowStockItems = lowStockItems.subList(0, 6);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("skuCount", products.size());
        summary.put("onHand", onHandTotal);
        summary.put("available", availableTotal);
        summary.put("outOfStock", outOfStock);

        return new InventoryComputed(summary, lowStockItems);
    }

    private List<Map<String, Object>> buildRevenueBars(
            String range,
            List<AdminOrderResponse> onlineOrders,
            List<Map<String, Object>> posOrders) {

        if ("today".equals(range)) {
            List<Map<String, Object>> buckets = new ArrayList<>();
            LocalDate today = LocalDate.now(ZONE);
            for (int i = 0; i < 8; i++) {
                buckets.add(new HashMap<>(Map.of(
                        "label", String.format("%02d", i * 3),
                        "value", 0L,
                        "startHour", i * 3,
                        "endHour", (i + 1) * 3)));
            }

            for (AdminOrderResponse order : onlineOrders) {
                addAmountToTodayBucket(buckets, parseInstant(order.createdAt()), toLong(order.totalAmount()), today);
            }
            for (Map<String, Object> order : posOrders) {
                addAmountToTodayBucket(
                        buckets,
                        parseInstant(asString(order.get("createdAt"))),
                        toLong(order.get("totalAmount")),
                        today);
            }

            return buckets.stream()
                    .map(b -> Map.of("label", b.get("label"), "value", b.get("value")))
                    .toList();
        }

        int days = "month".equals(range) ? 30 : 7;
        LocalDate start = LocalDate.now(ZONE).minusDays(days - 1L);
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            points.add(new HashMap<>(Map.of(
                    "label", DAY_LABEL.format(day),
                    "value", 0L,
                    "day", day.toString())));
        }

        for (AdminOrderResponse order : onlineOrders) {
            addAmountToDayPoint(points, parseInstant(order.createdAt()), toLong(order.totalAmount()), start, days);
        }
        for (Map<String, Object> order : posOrders) {
            addAmountToDayPoint(
                    points,
                    parseInstant(asString(order.get("createdAt"))),
                    toLong(order.get("totalAmount")),
                    start,
                    days);
        }

        if (!"month".equals(range)) {
            return points.stream().map(p -> Map.of("label", p.get("label"), "value", p.get("value"))).toList();
        }

        List<Map<String, Object>> reduced = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            int from = i * 5;
            int to = Math.min(points.size(), from + 5);
            if (from >= to) {
                continue;
            }

            List<Map<String, Object>> chunk = points.subList(from, to);
            long value = chunk.stream().mapToLong(row -> toLong(row.get("value"))).sum();
            String label = chunk.get(0).get("label") + "-" + chunk.get(chunk.size() - 1).get("label");
            reduced.add(Map.of("label", label, "value", value));
        }

        return reduced;
    }

    private List<Map<String, Object>> buildTopProducts(
            List<AdminOrderResponse> onlineOrders,
            List<Map<String, Object>> posOrders) {
        Map<String, Integer> agg = new HashMap<>();

        for (AdminOrderResponse order : onlineOrders) {
            if (order.items() == null) {
                continue;
            }
            for (AdminOrderItem item : order.items()) {
                String name = asString(item.productName(), "Sản phẩm");
                agg.put(name, agg.getOrDefault(name, 0) + Math.max(item.quantity(), 0));
            }
        }

        for (Map<String, Object> order : posOrders) {
            Object rawItems = order.get("items");
            if (!(rawItems instanceof List<?> list)) {
                continue;
            }
            for (Object rawItem : list) {
                if (!(rawItem instanceof Map<?, ?> item)) {
                    continue;
                }
                String name = asString(item.get("productName"), asString(item.get("name"), "Sản phẩm"));
                int qty = toInt(item.get("quantity"));
                agg.put(name, agg.getOrDefault(name, 0) + qty);
            }
        }

        return agg.entrySet().stream()
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "count", entry.getValue()))
                .sorted((a, b) -> Integer.compare(toInt(b.get("count")), toInt(a.get("count"))))
                .limit(6)
                .toList();
    }

    private List<Map<String, Object>> buildRecentOnlineOrders(List<AdminOrderResponse> onlineOrders) {
        return onlineOrders.stream()
                .sorted((a, b) -> {
                    Instant ta = parseInstant(a.createdAt());
                    Instant tb = parseInstant(b.createdAt());
                    return safeInstantCompareDesc(ta, tb);
                })
                .limit(8)
                .map(order -> {
                    String customer = "Khách hàng";
                    if (order.shippingAddress() != null && order.shippingAddress().fullName() != null
                            && !order.shippingAddress().fullName().isBlank()) {
                        customer = order.shippingAddress().fullName();
                    } else if (order.userId() != null) {
                        customer = String.valueOf(order.userId());
                    }

                    return Map.<String, Object>of(
                            "id", order.id(),
                            "code", "#" + shortCode(order.id()),
                            "customer", customer,
                            "totalAmount", toLong(order.totalAmount()),
                            "status", asString(order.status(), ""),
                            "statusLabel", statusLabel(order.status()),
                            "createdAt", asString(order.createdAt(), ""));
                })
                .toList();
    }

    private int safeInstantCompareDesc(Instant a, Instant b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return b.compareTo(a);
    }

    private void addAmountToTodayBucket(List<Map<String, Object>> buckets, Instant time, long amount, LocalDate today) {
        if (time == null) {
            return;
        }
        LocalDateTime ldt = LocalDateTime.ofInstant(time, ZONE);
        if (!ldt.toLocalDate().equals(today)) {
            return;
        }
        int idx = Math.min(7, Math.max(0, ldt.getHour() / 3));
        Map<String, Object> bucket = buckets.get(idx);
        bucket.put("value", toLong(bucket.get("value")) + amount);
    }

    private void addAmountToDayPoint(List<Map<String, Object>> points, Instant time, long amount, LocalDate start,
            int days) {
        if (time == null) {
            return;
        }
        LocalDate d = LocalDateTime.ofInstant(time, ZONE).toLocalDate();
        int idx = (int) (d.toEpochDay() - start.toEpochDay());
        if (idx < 0 || idx >= days) {
            return;
        }
        Map<String, Object> point = points.get(idx);
        point.put("value", toLong(point.get("value")) + amount);
    }

    private int sumPosItemsQty(Map<String, Object> order) {
        Object rawItems = order.get("items");
        if (!(rawItems instanceof List<?> list)) {
            return 0;
        }
        int qty = 0;
        for (Object rawItem : list) {
            if (rawItem instanceof Map<?, ?> m) {
                qty += toInt(m.get("quantity"));
            }
        }
        return qty;
    }

    private int resolveThreshold(Map<String, Object> product) {
        int defaultThreshold = 20;
        Object attributesRaw = product.get("attributes");

        if (attributesRaw == null) {
            return defaultThreshold;
        }

        try {
            Map<String, Object> attrs = parseAttributes(attributesRaw);
            if (attrs.containsKey("threshold")) {
                return Math.max(0, toInt(attrs.get("threshold")));
            }
            if (attrs.containsKey("reorderPoint")) {
                return Math.max(0, toInt(attrs.get("reorderPoint")));
            }
            if (attrs.containsKey("minStock")) {
                return Math.max(0, toInt(attrs.get("minStock")));
            }
        } catch (RuntimeException ex) {
            logger.debug("Failed to parse product attributes for threshold. productId={}", product.get("id"), ex);
        }

        return defaultThreshold;
    }

    private Map<String, Object> parseAttributes(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception ex) {
                return Map.of();
            }
        }
        return Map.of();
    }

    private boolean isOnlineRevenueStatus(String status) {
        String s = normalizeStatus(status);
        return "SHIPPING".equals(s) || "COMPLETED".equals(s);
    }

    private boolean isPosRevenueStatus(String status) {
        return "PAID".equals(normalizeStatus(status));
    }

    private boolean isPendingOnline(String status) {
        String s = normalizeStatus(status);
        return "DRAFT".equals(s) || "PENDING_PAYMENT".equals(s) || "PLACED".equals(s) || "CONFIRMED".equals(s);
    }

    private String statusLabel(String status) {
        String s = normalizeStatus(status);
        if ("COMPLETED".equals(s)) {
            return "Hoàn thành";
        }
        if ("SHIPPING".equals(s)) {
            return "Đang giao";
        }
        if ("CANCELED".equals(s) || "CANCELLED".equals(s)) {
            return "Đã hủy";
        }
        if ("PENDING_PAYMENT".equals(s) || "DRAFT".equals(s)) {
            return "Chờ thanh toán";
        }
        return "Đang xử lý";
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private DateRange currentRange(String range) {
        LocalDate today = LocalDate.now(ZONE);
        if ("today".equals(range)) {
            return new DateRange(today.atStartOfDay(ZONE).toInstant(),
                    today.plusDays(1).atStartOfDay(ZONE).toInstant());
        }
        int days = "month".equals(range) ? 30 : 7;
        LocalDate start = today.minusDays(days - 1L);
        return new DateRange(start.atStartOfDay(ZONE).toInstant(), today.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    private DateRange previousRange(String range, DateRange current) {
        if ("today".equals(range)) {
            return new DateRange(current.start.minusSeconds(24L * 60L * 60L), current.start);
        }
        int days = "month".equals(range) ? 30 : 7;
        long windowSeconds = days * 24L * 60L * 60L;
        return new DateRange(current.start.minusSeconds(windowSeconds), current.start);
    }

    private boolean isInRange(Instant instant, DateRange range) {
        return instant != null && !instant.isBefore(range.start) && instant.isBefore(range.end);
    }

    private String normalizeRange(String range) {
        String candidate = range == null ? "today" : range.trim().toLowerCase(Locale.ROOT);
        if ("today".equals(candidate) || "week".equals(candidate) || "month".equals(candidate)) {
            return candidate;
        }
        return "today";
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            // Try fallback formats below.
        }

        try {
            return LocalDateTime.parse(raw).atZone(ZONE).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try date-only input.
        }

        try {
            LocalDate date = LocalDate.parse(raw);
            return date.atTime(LocalTime.MIN).atZone(ZONE).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private <T> T safeFetch(Supplier<T> supplier, String source, T fallback) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            logger.warn("Dashboard analytics fallback due to {} fetch error", source, ex);
            return fallback;
        }
    }

    private int adminItemQty(AdminOrderItem item) {
        if (item == null) {
            return 0;
        }
        return Math.max(item.quantity(), 0);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return Math.round(number.doubleValue());
        }
        try {
            return Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Double pctChange(long current, long previous) {
        if (previous == 0L) {
            return current == 0L ? 0.0 : 100.0;
        }
        return ((current - (double) previous) / previous) * 100.0;
    }

    private UUID asUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof UUID uuid) {
            return uuid;
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String asString(Object raw) {
        return asString(raw, "");
    }

    private String asString(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String out = String.valueOf(raw);
        return out.isBlank() ? fallback : out;
    }

    private String shortCode(UUID id) {
        if (id == null) {
            return "";
        }
        String s = id.toString();
        return s.substring(0, Math.min(8, s.length()));
    }

    private record DateRange(Instant start, Instant end) {
    }

    private record InventoryComputed(Map<String, Object> inventorySummary, List<Map<String, Object>> lowStockItems) {
    }
}
