package com.backend.content.repo;

import com.backend.content.model.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import com.backend.content.model.ModerationStatus;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    Page<Answer> findByThreadId(UUID threadId, Pageable pageable);

    List<Answer> findByThreadId(UUID threadId);

    @Query("select a from Answer a where a.threadId = :threadId order by a.bestAnswer desc, a.pinned desc, a.createdAt desc")
    Page<Answer> findBestByThreadId(@Param("threadId") UUID threadId, Pageable pageable);

    List<Answer> findTop100ByModerationStatusOrderByCreatedAtDesc(ModerationStatus status);

    @Query("select a.threadId as threadId, count(a) as total from Answer a where a.threadId in :threadIds group by a.threadId")
    List<ThreadAnswerCount> countByThreadIds(@Param("threadIds") List<UUID> threadIds);

    @Query("select a.threadId as threadId, count(a) as total from Answer a where a.threadId in :threadIds and a.moderationStatus = :status group by a.threadId")
    List<ThreadAnswerCount> countByThreadIdsAndStatus(@Param("threadIds") List<UUID> threadIds,
            @Param("status") ModerationStatus status);
}
