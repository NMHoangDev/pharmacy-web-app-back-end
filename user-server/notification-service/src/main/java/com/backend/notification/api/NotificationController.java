package com.backend.notification.api;

import com.backend.notification.api.dto.CreateBroadcastNotificationRequest;
import com.backend.notification.api.dto.CreateUserNotificationRequest;
import com.backend.notification.api.dto.NotificationListResponse;
import com.backend.notification.api.dto.NotificationResponse;
import com.backend.notification.api.dto.NotificationUnreadCountResponse;
import com.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.getCurrentUserNotifications(limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCountV2() {
        return ResponseEntity.ok(new NotificationUnreadCountResponse(notificationService.getCurrentUserUnreadCount()));
    }

    @PostMapping("/me")
    public ResponseEntity<NotificationResponse> createMyNotification(
            @Valid @RequestBody CreateUserNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createCurrentUserNotification(
                request.category(),
                request.title(),
                request.message(),
                request.sourceType(),
                request.sourceId(),
                request.sourceEventType(),
                request.actionUrl()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsReadV2(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("id", notificationId, "read", true));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsReadV2() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteForCurrentUser(notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<NotificationListResponse> getMyNotifications(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return getNotifications(limit);
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount() {
        return getUnreadCountV2();
    }

    @PostMapping("/me/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable UUID notificationId) {
        return markAsReadV2(notificationId);
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        return markAllAsReadV2();
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN','MOD','EDITOR')")
    public ResponseEntity<NotificationResponse> broadcastNotification(
            @Valid @RequestBody CreateBroadcastNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createBroadcastNotification(
                request.category(),
                request.title(),
                request.message(),
                request.targetRole(),
                request.sourceType(),
                request.sourceId(),
                request.actionUrl()));
    }
}