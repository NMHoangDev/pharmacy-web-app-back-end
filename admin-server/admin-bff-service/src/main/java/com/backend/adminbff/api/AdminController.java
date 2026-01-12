package com.backend.adminbff.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

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
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> dashboardSummary() {
        return ResponseEntity.ok(Map.of(
                "ordersToday", 0,
                "orders7d", 0,
                "orders30d", 0,
                "revenueToday", 0,
                "revenue30d", 0,
                "pendingOrders", 0,
                "lowStock", 0,
                "reviewsPending", 0,
                "appointmentsToday", 0,
                "newUsers", 0,
                "topProducts", List.of()));
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
        return ResponseEntity
                .ok(Map.of("status", status, "userId", userId, "page", page, "size", size, "items", List.of()));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "items", List.of()));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "status", "CANCEL_REQUESTED"));
    }

    @PostMapping("/orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(@PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(Map.of("id", id, "status", status));
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
    public ResponseEntity<Map<String, Object>> listUsers(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of("q", q, "page", page, "size", size, "items", List.of()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("id", id, "profile", Map.of(), "addresses", List.of()));
    }

    @PostMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(Map.of("id", id, "status", status));
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
