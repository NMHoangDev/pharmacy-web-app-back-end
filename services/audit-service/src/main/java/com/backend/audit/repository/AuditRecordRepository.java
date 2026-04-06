package com.backend.audit.repository;

import com.backend.audit.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    Page<AuditRecord> findByActorContainsIgnoreCase(String actor, Pageable pageable);

    Page<AuditRecord> findByActionContainsIgnoreCase(String action, Pageable pageable);

    Page<AuditRecord> findByResourceContainsIgnoreCase(String resource, Pageable pageable);
}
