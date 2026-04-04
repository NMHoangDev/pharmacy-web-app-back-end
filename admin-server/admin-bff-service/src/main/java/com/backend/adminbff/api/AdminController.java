package com.backend.adminbff.api;

import com.backend.adminbff.client.AdminIdentityClient;
import com.backend.adminbff.client.AdminOrderClient;
import com.backend.adminbff.client.AdminUserClient;
import com.backend.adminbff.dto.AdminOrderResponse;
import com.backend.adminbff.dto.AdminUserIdentitySummary;
import com.backend.adminbff.dto.AdminUserListItem;
import com.backend.adminbff.dto.AdminUserProfile;
import com.backend.adminbff.dto.UserOrderCountResponse;
import com.backend.adminbff.dto.UpsertAdminUserRequest;
import com.backend.adminbff.service.DashboardAnalyticsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminUserClient adminUserClient;
    private final AdminIdentityClient adminIdentityClient;
    private final AdminOrderClient adminOrderClient;
    private final DashboardAnalyticsService dashboardAnalyticsService;

    public AdminController(
            AdminUserClient adminUserClient,
            AdminIdentityClient adminIdentityClient,
            AdminOrderClient adminOrderClient,
            DashboardAnalyticsService dashboardAnalyticsService) {
        this.adminUserClient = adminUserClient;
        this.adminIdentityClient = adminIdentityClient;
        this.adminOrderClient = adminOrderClient;
        this.dashboardAnalyticsService = dashboardAnalyticsService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("admin-bff ok");
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "X-Request-Id", required = false) String rid,
            @RequestHeader(value = "User-Agent", required = false) String ua,
            @RequestHeader(value = "Host", required = false) String host,
            @RequestHeader(value = "Accept-Language", required = false) String lang,
            @RequestHeader(value = "Accept", required = false) String accept,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xff,
            @RequestHeader(value = "X-Real-IP", required = false) String rip,
            java.security.Principal principal) {
        return ResponseEntity.ok(Map.of(
                "user", principal != null ? principal.getName() : "anonymous",
                "roles", List.of("ADMIN"),
                "requestId", rid,
                "clientIp", rip != null ? rip : xff,
                "ua", ua,
                "host", host,
                "lang", lang,
                "accept", accept,
                "authProvided", auth != null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> permissions() {
        return ResponseEntity.ok(Map.of(
                "permissions", List.of(
                        "dashboard:view",
                        "orders:read", "orders:write",
                        "inventory:write",
                        "catalog:write",
                        "users:read", "users:write",
                        "reviews:moderate",
                        "appointments:write",
                        "notifications:send",
                        "audit:read")));
    }

    // Dashboard
    @GetMapping("/dashboard/analytics")
    public ResponseEntity<Map<String, Object>> dashboardAnalytics(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(dashboardAnalyticsService.getAnalytics(range, branchId));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> dashboardSummary() {
        // Map.of supports up to 10 entries; Map.ofEntries keeps this response literal
        // but valid.
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("ordersToday", 0),
                Map.entry("orders7d", 0),
                Map.entry("orders30d", 0),
                Map.entry("revenueToday", 0),
                Map.entry("revenue30d", 0),
                Map.entry("pendingOrders", 0),
                Map.entry("lowStock", 0),
                Map.entry("reviewsPending", 0),
                Map.entry("appointmentsToday", 0),
                Map.entry("newUsers", 0),
                Map.entry("topProducts", List.of())));
    }

    @GetMapping("/dashboard/timeseries")
    public ResponseEntity<Map<String, Object>> dashboardTimeseries(@RequestParam(defaultValue = "orders") String metric,
            @RequestParam(defaultValue = "7d") String range) {
        return ResponseEntity.ok(Map.of(
                "metric", metric,
                "range", range,
                "points", List.of()));
    }

    @GetMapping("/dashboard/alerts")
    public ResponseEntity<Map<String, Object>> dashboardAlerts() {
        return ResponseEntity.ok(Map.of("alerts", List.of()));
    }

    // Search
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "orders,users,products") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "q", q,
                "types", type,
                "page", page,
                "size", size,
                "results", List.of()));
    }

    // Orders
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> listOrders(@RequestParam(required = false) String status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Admin listOrders status={} userId={} page={} size={}", status, userId, page, size);
        List<AdminOrderResponse> items = adminOrderClient.listOrders(status, userId);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("status", status);
        response.put("userId", userId);
        response.put("page", page);
        response.put("size", size);
        response.put("items", items);
        response.put("total", items.size());

        logger.info("Admin listOrders result count={}", items.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<AdminOrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOrderClient.getOrder(id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<AdminOrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOrderClient.cancelOrder(id));
    }

    @PostMapping("/orders/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateOrderStatus(@PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(adminOrderClient.updateStatus(id, status));
    }

    // Inventory
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<Map<String, Object>> lowStock() {
        return ResponseEntity.ok(Map.of("items", List.of()));
    }

    @PostMapping("/inventory/{productId}/adjust")
    public ResponseEntity<Map<String, Object>> adjustInventory(@PathVariable UUID productId,
            @RequestParam int delta) {
        return ResponseEntity.ok(Map.of("productId", productId, "delta", delta));
    }

    // Products
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> listProducts(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(Map.of("page", page, "size", size, "q", q, "items", List.of()));
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("created", true, "payload", body));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", id, "updated", true, "payload", body));
    }

    @PostMapping("/products/{id}/media")
    public ResponseEntity<Map<String, Object>> attachMedia(@PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("id", id, "media", body));
    }

    // Users
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserListItem>> listUsers() {
        List<AdminUserProfile> users = adminUserClient.listUsers();
        Map<UUID, AdminUserIdentitySummary> identityByUserId = adminIdentityClient.listUserIdentitySummaries().stream()
                .collect(Collectors.toMap(
                        AdminUserIdentitySummary::id,
                        identity -> identity,
                        (left, right) -> left));
        Map<UUID, Long> orderCountByUserId = adminOrderClient.countPlacedOrdersByUser().stream()
                .collect(Collectors.toMap(
                        UserOrderCountResponse::userId,
                        UserOrderCountResponse::orderCount,
                        Long::sum));

        List<AdminUserListItem> items = users.stream()
                .map(user -> {
                    AdminUserIdentitySummary identity = identityByUserId.get(user.id());
                    return new AdminUserListItem(
                            user.id(),
                            user.email(),
                            user.phone(),
                            user.fullName(),
                            user.fullName(),
                            user.avatarBase64(),
                            user.createdAt(),
                            orderCountByUserId.getOrDefault(user.id(), 0L),
                            identity != null ? identity.role() : "customer",
                            identity != null ? identity.status() : "active",
                            identity != null ? identity.keycloakRoles() : List.of(),
                            identity == null || identity.enabled(),
                            identity != null && identity.emailVerified());
                })
                .sorted((left, right) -> compareCreatedAtDesc(left.createdAt(), right.createdAt()))
                .toList();

        logger.info("Admin listUsers response size={}, identityCount={}, orderCountEntries={}",
                items.size(),
                identityByUserId.size(),
                orderCountByUserId.size());

        return ResponseEntity.ok(items);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserProfile> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserClient.getUser(id));
    }

    @PostMapping("/users")
    public ResponseEntity<AdminUserProfile> createUser(@RequestBody @Valid UpsertAdminUserRequest request) {
        return ResponseEntity.ok(adminUserClient.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserProfile> updateUser(@PathVariable UUID id,
            @RequestBody @Valid UpsertAdminUserRequest request) {
        return ResponseEntity.ok(adminUserClient.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminUserClient.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(Map.of("id", id, "status", status));
    }

    private static int compareCreatedAtDesc(java.time.Instant left, java.time.Instant right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private static boolean isPlacedOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status.trim().toUpperCase()) {
            case "PENDING_PAYMENT", "PLACED", "CONFIRMED", "SHIPPING", "COMPLETED" -> true;
            case "DRAFT", "CANCELED" -> false;
            default -> false;
        };
    }

    // Reviews
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> listReviews(@RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of("status", status, "productId", productId, "userId", userId, "page", page,
                "size", size, "items", List.of()));
    }

    @PostMapping("/reviews/{id}/status")
    public ResponseEntity<Map<String, Object>> updateReviewStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(Map.of("id", id, "status", status));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }

    // Appointments
    @GetMapping("/appointments")
    public ResponseEntity<Map<String, Object>> listAppointments(@RequestParam(required = false) UUID pharmacistId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                Map.of("pharmacistId", pharmacistId, "status", status, "page", page, "size", size, "items", List.of()));
    }

    @PostMapping("/appointments/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirmAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "CONFIRMED"));
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "CANCELLED"));
    }

    // Notifications
    @PostMapping("/notifications/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestParam String to) {
        return ResponseEntity.ok(Map.of("sent", true, "to", to));
    }

    @PostMapping("/notifications/test-sms")
    public ResponseEntity<Map<String, Object>> testSms(@RequestParam String to) {
        return ResponseEntity.ok(Map.of("sent", true, "to", to));
    }

    // Audit (placeholder)
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(@RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity
                .ok(Map.of("actor", actor, "action", action, "page", page, "size", size, "items", List.of()));
    }
}
