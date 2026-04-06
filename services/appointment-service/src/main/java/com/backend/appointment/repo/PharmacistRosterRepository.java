package com.backend.appointment.repo;

import com.backend.appointment.model.PharmacistRoster;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface PharmacistRosterRepository extends JpaRepository<PharmacistRoster, UUID> {
    List<PharmacistRoster> findByPharmacistId(UUID pharmacistId);

    List<PharmacistRoster> findByPharmacistIdAndDayOfWeek(UUID pharmacistId, int dayOfWeek);

    List<PharmacistRoster> findByPharmacistIdAndBranchIdAndDayOfWeek(UUID pharmacistId, UUID branchId, int dayOfWeek);

    @Query("select distinct r.pharmacistId from PharmacistRoster r")
    List<UUID> findDistinctPharmacistIds();

    @Query("select distinct r.pharmacistId from PharmacistRoster r where r.branchId = :branchId")
    List<UUID> findDistinctPharmacistIdsByBranchId(@Param("branchId") UUID branchId);
}