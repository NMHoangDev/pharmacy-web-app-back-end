package com.backend.ai.repo;

import com.backend.ai.model.ChatConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {
}
