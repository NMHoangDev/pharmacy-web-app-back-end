package com.backend.appointment.repo;

import com.backend.appointment.model.AppointmentAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentAuditLogRepository extends JpaRepository<AppointmentAuditLog, UUID> {
    List<AppointmentAuditLog> findByAppointmentIdOrderByCreatedAtDesc(UUID appointmentId);
}