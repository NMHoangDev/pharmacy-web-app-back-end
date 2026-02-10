package com.backend.catalog.repo;

import com.backend.catalog.model.DrugBranchSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DrugBranchSettingRepository extends JpaRepository<DrugBranchSetting, UUID> {
    Optional<DrugBranchSetting> findByDrugIdAndBranchId(UUID drugId, UUID branchId);
}
