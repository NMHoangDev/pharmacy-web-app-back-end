package com.backend.notification.api;

import com.backend.notification.api.dto.CreateBroadcastNotificationRequest;
import com.backend.notification.api.dto.NotificationListResponse;
import com.backend.notification.api.dto.NotificationResponse;
import com.backend.notification.api.dto.NotificationUnreadCountResponse;
import com.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/me")
    public ResponseEntity<NotificationListResponse> getMyNotifications(
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.getCurrentUserNotifications(limit));
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(new NotificationUnreadCountResponse(notificationService.getCurrentUserUnreadCount()));
    }

    @PostMapping("/me/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(Map.of("id", notificationId, "read", true));
    }

    @PostMapping("/me/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
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