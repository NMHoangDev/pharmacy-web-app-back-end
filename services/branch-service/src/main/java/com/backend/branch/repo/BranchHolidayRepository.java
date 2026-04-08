package com.backend.branch.repo;

import com.backend.branch.model.BranchHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchHolidayRepository extends JpaRepository<BranchHoliday, UUID> {
    List<BranchHoliday> findByBranchId(UUID branchId);
}
