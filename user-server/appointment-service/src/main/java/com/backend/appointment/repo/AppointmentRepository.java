package com.backend.appointment.repo;

import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByUserId(UUID userId, Pageable pageable);

    Page<Appointment> findByPharmacistId(UUID pharmacistId, Pageable pageable);

    boolean existsByPharmacistIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(UUID pharmacistId, LocalDateTime start,
            LocalDateTime end);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    java.util.List<Appointment> findByPharmacistIdAndStartAtBetween(UUID pharmacistId, LocalDateTime from,
            LocalDateTime to);

    boolean existsByPharmacistIdAndStartAtAndStatusNot(UUID pharmacistId, LocalDateTime startAt,
            AppointmentStatus statusToExclude);
}
