package com.backend.consultation.repo;

import com.backend.consultation.model.ConsultationAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsultationAppointmentRepository extends JpaRepository<ConsultationAppointment, UUID>,
        JpaSpecificationExecutor<ConsultationAppointment> {
    boolean existsByPharmacistIdAndStartAt(UUID pharmacistId, LocalDateTime startAt);

    List<ConsultationAppointment> findByPharmacistIdAndStartAtBetween(UUID pharmacistId, LocalDateTime from,
            LocalDateTime to);
}
