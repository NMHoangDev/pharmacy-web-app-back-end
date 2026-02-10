package com.backend.branch.repo;

import com.backend.branch.model.BranchSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchSettingsRepository extends JpaRepository<BranchSettings, UUID> {
}
