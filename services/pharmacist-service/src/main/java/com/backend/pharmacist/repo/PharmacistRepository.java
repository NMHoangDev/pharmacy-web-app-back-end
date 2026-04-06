package com.backend.pharmacist.repo;

import com.backend.pharmacist.model.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PharmacistRepository extends JpaRepository<Pharmacist, UUID>, JpaSpecificationExecutor<Pharmacist> {
}
