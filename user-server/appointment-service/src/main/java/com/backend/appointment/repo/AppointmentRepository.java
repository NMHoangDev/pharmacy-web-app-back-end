package com.backend.appointment.repo;

import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
        List<Appointment> findByUserId(UUID userId);

        Page<Appointment> findByUserId(UUID userId, Pageable pageable);

        Page<Appointment> findByUserIdAndBranchId(UUID userId, UUID branchId, Pageable pageable);

        List<Appointment> findByPharmacistId(UUID pharmacistId);

        Page<Appointment> findByPharmacistId(UUID pharmacistId, Pageable pageable);

        Page<Appointment> findByPharmacistIdAndBranchId(UUID pharmacistId, UUID branchId, Pageable pageable);

        List<Appointment> findByStartAtBetween(LocalDateTime startAt, LocalDateTime endAt);

        List<Appointment> findByStartAtBetweenAndBranchId(LocalDateTime startAt, LocalDateTime endAt,
                        UUID branchId);

        boolean existsByPharmacistIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        UUID pharmacistId,
                        LocalDateTime startAt,
                        LocalDateTime endAt);

        boolean existsByPharmacistIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        UUID pharmacistId,
                        List<AppointmentStatus> statuses,
                        LocalDateTime startAt,
                        LocalDateTime endAt);

        boolean existsByPharmacistIdAndBranchIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        UUID pharmacistId,
                        UUID branchId,
                        List<AppointmentStatus> statuses,
                        LocalDateTime startAt,
                        LocalDateTime endAt);

        @Modifying
        @Transactional
        @Query("UPDATE Appointment a SET a.status = :newStatus, a.updatedAt = CURRENT_TIMESTAMP WHERE a.status IN :statuses AND a.endAt < :threshold")
        int cancelExpiredAppointments(@Param("statuses") List<AppointmentStatus> statuses,
                        @Param("threshold") LocalDateTime threshold,
                        @Param("newStatus") AppointmentStatus newStatus);

        List<Appointment> findByStatusInAndEndAtBefore(List<AppointmentStatus> statuses, LocalDateTime threshold);

        long countByUserIdAndStatusInAndCreatedAtAfter(UUID userId, List<AppointmentStatus> statuses,
                        java.time.Instant after);

        long countByPharmacistIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        UUID pharmacistId,
                        List<AppointmentStatus> statuses,
                        LocalDateTime startAt,
                        LocalDateTime endAt);

        long countByPharmacistIdAndBranchIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        UUID pharmacistId,
                        UUID branchId,
                        List<AppointmentStatus> statuses,
                        LocalDateTime startAt,
                        LocalDateTime endAt);
}
