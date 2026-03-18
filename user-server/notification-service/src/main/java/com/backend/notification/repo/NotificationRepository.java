package com.backend.notification.repo;

import com.backend.notification.model.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

        @Query("""
                        select n from NotificationEntity n
                                    where (n.recipientUserId = :userId
                                       or (n.audience = 'BROADCAST' and (n.targetRole is null or upper(n.targetRole) in :roles)))
                                      and not exists (
                                              select 1 from NotificationReceipt rd
                                              where rd.notificationId = n.id and rd.userId = :userId and rd.deletedAt is not null
                                      )
                        order by n.createdAt desc
                        """)
        List<NotificationEntity> findVisibleForUser(
                        @Param("userId") UUID userId,
                        @Param("roles") Collection<String> roles,
                        Pageable pageable);

        @Query("""
                        select count(n) from NotificationEntity n
                        where (n.recipientUserId = :userId
                           or (n.audience = 'BROADCAST' and (n.targetRole is null or upper(n.targetRole) in :roles)))
                                      and not exists (
                                              select 1 from NotificationReceipt rd
                                              where rd.notificationId = n.id and rd.userId = :userId and rd.deletedAt is not null
                                      )
                          and not exists (
                              select 1 from NotificationReceipt r
                                              where r.notificationId = n.id and r.userId = :userId and r.deletedAt is null
                          )
                        """)
        long countUnreadVisibleForUser(
                        @Param("userId") UUID userId,
                        @Param("roles") Collection<String> roles);
}