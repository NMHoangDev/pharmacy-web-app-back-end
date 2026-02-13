package com.backend.content.repo;

import com.backend.content.model.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostImageRepository extends JpaRepository<PostImage, UUID> {
    List<PostImage> findByPostIdOrderByPositionAsc(UUID postId);

    void deleteByPostId(UUID postId);
}
