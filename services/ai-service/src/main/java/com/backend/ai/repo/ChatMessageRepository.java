package com.backend.ai.repo;

import com.backend.ai.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    List<ChatMessageEntity> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);
}
