package com.backend.branch.repo;

import com.backend.branch.model.BranchAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchAuditLogRepository extends JpaRepository<BranchAuditLog, UUID> {
    List<BranchAuditLog> findByBranchIdOrderByCreatedAtDesc(UUID branchId);
}
