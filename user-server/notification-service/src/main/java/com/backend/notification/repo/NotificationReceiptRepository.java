package com.backend.notification.repo;

import com.backend.notification.model.NotificationReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationReceiptRepository extends JpaRepository<NotificationReceipt, UUID> {

    List<NotificationReceipt> findByUserIdAndNotificationIdIn(UUID userId, Collection<UUID> notificationIds);

    List<NotificationReceipt> findByUserIdAndNotificationIdInAndDeletedAtIsNull(UUID userId,
            Collection<UUID> notificationIds);

    Optional<NotificationReceipt> findByUserIdAndNotificationId(UUID userId, UUID notificationId);

    Optional<NotificationReceipt> findByUserIdAndNotificationIdAndDeletedAtIsNull(UUID userId, UUID notificationId);
}