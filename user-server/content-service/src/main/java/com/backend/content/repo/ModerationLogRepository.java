package com.backend.content.repo;

import com.backend.content.model.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {
}
