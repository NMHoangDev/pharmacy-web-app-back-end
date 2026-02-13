package com.backend.content.repo;

import com.backend.content.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.backend.content.model.ModerationStatus;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Modifying
    @Query("update Post p set p.views = p.views + 1 where p.id = :id")
    int incrementViews(@Param("id") UUID id);

    List<Post> findTop5ByModerationStatusAndIdNotOrderByPublishedAtDesc(ModerationStatus status, UUID id);

    List<Post> findTop100ByModerationStatusOrderByCreatedAtDesc(ModerationStatus status);
}
