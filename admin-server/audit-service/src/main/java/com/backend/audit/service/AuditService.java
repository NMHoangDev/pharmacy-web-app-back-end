package com.backend.audit.service;

import com.backend.audit.api.dto.AuditRecordRequest;
import com.backend.audit.model.AuditRecord;
import com.backend.audit.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditService {

    private final AuditRecordRepository repository;

    public AuditRecord record(AuditRecordRequest request) {
        AuditRecord record = new AuditRecord();
        record.setActor(request.actor());
        record.setAction(request.action());
        record.setResource(request.resource());
        record.setMetadata(request.metadata());
        return repository.save(record);
    }

    public Page<AuditRecord> search(String actor, String action, String resource, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (actor != null && !actor.isBlank()) {
            return repository.findByActorContainsIgnoreCase(actor, pageable);
        }
        if (action != null && !action.isBlank()) {
            return repository.findByActionContainsIgnoreCase(action, pageable);
        }
        if (resource != null && !resource.isBlank()) {
            return repository.findByResourceContainsIgnoreCase(resource, pageable);
        }
        return repository.findAll(pageable);
    }
}
