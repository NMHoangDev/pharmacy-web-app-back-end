package com.backend.pharmacist.repo;

import com.backend.pharmacist.model.PharmacistShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PharmacistShiftRepository extends JpaRepository<PharmacistShift, UUID> {
    List<PharmacistShift> findByPharmacistId(UUID pharmacistId);

    List<PharmacistShift> findByPharmacistIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
            UUID pharmacistId, LocalDateTime from, LocalDateTime to);
}
