package com.backend.content.repo;

import com.backend.content.model.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ThreadRepository extends JpaRepository<Thread, UUID>, JpaSpecificationExecutor<Thread> {
    Optional<Thread> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Thread> findTop100ByModerationStatusOrderByCreatedAtDesc(com.backend.content.model.ModerationStatus status);
}
