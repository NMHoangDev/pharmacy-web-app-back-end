package com.backend.appointment.service;

import com.backend.appointment.model.AppointmentAuditLog;
import com.backend.appointment.repo.AppointmentAuditLogRepository;
import com.backend.appointment.security.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AppointmentAuditService {

    private final AppointmentAuditLogRepository repository;

    public AppointmentAuditService(AppointmentAuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(UUID appointmentId, String action, String beforeStatus, String afterStatus, String reason,
            String actorRole, String actorIp, String metadata) {
        AppointmentAuditLog log = new AppointmentAuditLog();
        log.setId(UUID.randomUUID());
        log.setAppointmentId(appointmentId);
        log.setAction(action);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setReason(reason);
        log.setActorId(SecurityUtils.getActorId());
        log.setActorRole(actorRole);
        log.setActorIp(actorIp);
        log.setMetadata(metadata);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }
}