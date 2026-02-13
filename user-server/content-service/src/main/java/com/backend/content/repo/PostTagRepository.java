package com.backend.content.repo;

import com.backend.content.model.PostTag;
import com.backend.content.model.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
    List<PostTag> findByIdPostIdIn(List<UUID> postIds);

    void deleteByIdPostId(UUID postId);
}
