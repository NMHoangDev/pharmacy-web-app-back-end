package com.backend.notification.service;

import com.backend.notification.api.dto.NotificationListResponse;
import com.backend.notification.api.dto.NotificationResponse;
import com.backend.notification.model.NotificationEntity;
import com.backend.notification.model.NotificationReceipt;
import com.backend.notification.repo.NotificationReceiptRepository;
import com.backend.notification.repo.NotificationRepository;
import com.backend.notification.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    public static final String AUDIENCE_USER = "USER";
    public static final String AUDIENCE_BROADCAST = "BROADCAST";

    private final NotificationRepository notificationRepository;
    private final NotificationReceiptRepository notificationReceiptRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationReceiptRepository notificationReceiptRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationReceiptRepository = notificationReceiptRepository;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getCurrentUserNotifications(int limit) {
        UUID userId = requireCurrentUserId();
        Set<String> roles = resolveRoles();
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, safeLimit);

        List<NotificationEntity> notifications = notificationRepository.findVisibleForUser(userId, roles, pageable);
        Set<UUID> ids = notifications.stream().map(NotificationEntity::getId).collect(Collectors.toSet());
        Set<UUID> readIds = ids.isEmpty()
                ? Set.of()
                : notificationReceiptRepository.findByUserIdAndNotificationIdInAndDeletedAtIsNull(userId, ids)
                        .stream()
                        .map(NotificationReceipt::getNotificationId)
                        .collect(Collectors.toSet());

        List<NotificationResponse> items = notifications.stream()
                .map(notification -> toResponse(notification, readIds.contains(notification.getId())))
                .toList();

        return new NotificationListResponse(items, notificationRepository.countUnreadVisibleForUser(userId, roles));
    }

    @Transactional(readOnly = true)
    public long getCurrentUserUnreadCount() {
        return notificationRepository.countUnreadVisibleForUser(requireCurrentUserId(), resolveRoles());
    }

    public void markAsRead(UUID notificationId) {
        UUID userId = requireCurrentUserId();
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!isVisibleToUser(notification, userId, resolveRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to current user");
        }

        NotificationReceipt existing = notificationReceiptRepository
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElse(null);
        if (existing != null && existing.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }

        if (existing == null) {
            notificationReceiptRepository.save(newReceipt(userId, notificationId));
            return;
        }

        if (existing.getReadAt() == null) {
            existing.setReadAt(Instant.now());
            notificationReceiptRepository.save(existing);
        }
    }

    public int markAllAsRead() {
        UUID userId = requireCurrentUserId();
        Set<String> roles = resolveRoles();
        List<NotificationEntity> visibleNotifications = notificationRepository.findVisibleForUser(userId, roles,
                Pageable.unpaged());
        if (visibleNotifications.isEmpty()) {
            return 0;
        }

        Set<UUID> visibleIds = visibleNotifications.stream().map(NotificationEntity::getId).collect(Collectors.toSet());
        Set<UUID> readIds = notificationReceiptRepository.findByUserIdAndNotificationIdInAndDeletedAtIsNull(userId,
                visibleIds).stream()
                .map(NotificationReceipt::getNotificationId)
                .collect(Collectors.toSet());

        List<NotificationReceipt> newReceipts = visibleIds.stream()
                .filter(id -> !readIds.contains(id))
                .map(id -> newReceipt(userId, id))
                .toList();
        notificationReceiptRepository.saveAll(newReceipts);
        return newReceipts.size();
    }

    public void deleteForCurrentUser(UUID notificationId) {
        UUID userId = requireCurrentUserId();
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!isVisibleToUser(notification, userId, resolveRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to current user");
        }

        if (userId.equals(notification.getRecipientUserId())) {
            notificationRepository.delete(notification);
            return;
        }

        NotificationReceipt receipt = notificationReceiptRepository
                .findByUserIdAndNotificationId(userId, notificationId)
                .orElseGet(() -> newReceipt(userId, notificationId));
        if (receipt.getReadAt() == null) {
            receipt.setReadAt(Instant.now());
        }
        receipt.setDeletedAt(Instant.now());
        notificationReceiptRepository.save(receipt);
    }

    public NotificationResponse createUserNotification(
            UUID recipientUserId,
            String category,
            String title,
            String message,
            String sourceType,
            String sourceId,
            String sourceEventType,
            String actionUrl) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setRecipientUserId(recipientUserId);
        notification.setAudience(AUDIENCE_USER);
        notification.setTargetRole(null);
        notification.setCategory(normalize(category));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setSourceEventType(sourceEventType);
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(Instant.now());
        return toResponse(notificationRepository.save(notification), false);
    }

    public NotificationResponse createCurrentUserNotification(
            String category,
            String title,
            String message,
            String sourceType,
            String sourceId,
            String sourceEventType,
            String actionUrl) {
        UUID currentUserId = requireCurrentUserId();
        return createUserNotification(
                currentUserId,
                category,
                title,
                message,
                sourceType,
                sourceId,
                sourceEventType,
                actionUrl);
    }

    public NotificationResponse createBroadcastNotification(
            String category,
            String title,
            String message,
            String targetRole,
            String sourceType,
            String sourceId,
            String actionUrl) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setRecipientUserId(null);
        notification.setAudience(AUDIENCE_BROADCAST);
        notification.setTargetRole(targetRole == null || targetRole.isBlank() ? null : targetRole.toUpperCase());
        notification.setCategory(normalize(category));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSourceType(sourceType);
        notification.setSourceId(sourceId);
        notification.setSourceEventType("manual.broadcast");
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(Instant.now());
        return toResponse(notificationRepository.save(notification), false);
    }

    private NotificationReceipt newReceipt(UUID userId, UUID notificationId) {
        NotificationReceipt receipt = new NotificationReceipt();
        receipt.setId(UUID.randomUUID());
        receipt.setUserId(userId);
        receipt.setNotificationId(notificationId);
        receipt.setReadAt(Instant.now());
        return receipt;
    }

    private NotificationResponse toResponse(NotificationEntity notification, boolean read) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getSourceType(),
                notification.getSourceId(),
                notification.getSourceEventType(),
                notification.getActionUrl(),
                notification.getCreatedAt(),
                read);
    }

    private UUID requireCurrentUserId() {
        UUID userId = SecurityUtils.getActorId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userId;
    }

    private Set<String> resolveRoles() {
        Set<String> roles = SecurityUtils.getRoles();
        return roles.isEmpty() ? Set.of("__NONE__") : roles;
    }

    private boolean isVisibleToUser(NotificationEntity notification, UUID userId, Collection<String> roles) {
        if (userId.equals(notification.getRecipientUserId())) {
            return true;
        }
        return AUDIENCE_BROADCAST.equals(notification.getAudience())
                && (notification.getTargetRole() == null || roles.contains(notification.getTargetRole().toUpperCase()));
    }

    private String normalize(String value) {
        return value == null ? "SYSTEM" : value.trim().toUpperCase();
    }
}