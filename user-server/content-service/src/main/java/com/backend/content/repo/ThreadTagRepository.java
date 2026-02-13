package com.backend.content.repo;

import com.backend.content.model.ThreadTag;
import com.backend.content.model.ThreadTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ThreadTagRepository extends JpaRepository<ThreadTag, ThreadTagId> {
    List<ThreadTag> findByIdThreadIdIn(List<UUID> threadIds);

    void deleteByIdThreadId(UUID threadId);
}
