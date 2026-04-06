package com.backend.content.repo;

import com.backend.content.model.TargetType;
import com.backend.content.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    Optional<Vote> findByTargetTypeAndTargetIdAndUserId(TargetType targetType, UUID targetId, UUID userId);
}
