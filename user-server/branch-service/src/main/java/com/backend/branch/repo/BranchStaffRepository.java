package com.backend.branch.repo;

import com.backend.branch.model.BranchStaff;
import com.backend.branch.model.BranchStaffId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchStaffRepository extends JpaRepository<BranchStaff, BranchStaffId> {
    List<BranchStaff> findByIdBranchId(UUID branchId);

    List<BranchStaff> findByIdUserId(UUID userId);

    List<BranchStaff> findByIdBranchIdAndRole(UUID branchId, String role);
}
