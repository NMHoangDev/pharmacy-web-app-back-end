package com.backend.appointment.repo;

import com.backend.appointment.model.PharmacistTimeOff;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacistTimeOffRepository extends JpaRepository<PharmacistTimeOff, UUID> {
    List<PharmacistTimeOff> findByPharmacistId(UUID pharmacistId);

    List<PharmacistTimeOff> findByPharmacistIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            UUID pharmacistId,
            LocalDateTime startAt,
            LocalDateTime endAt);

    List<PharmacistTimeOff> findByPharmacistIdAndBranchIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            UUID pharmacistId,
            UUID branchId,
            LocalDateTime startAt,
            LocalDateTime endAt);
}