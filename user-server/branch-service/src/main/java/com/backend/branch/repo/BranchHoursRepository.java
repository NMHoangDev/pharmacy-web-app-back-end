package com.backend.branch.repo;

import com.backend.branch.model.BranchHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchHoursRepository extends JpaRepository<BranchHours, UUID> {
}
