package com.backend.user.repo;

import com.backend.user.model.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, UUID> {
}
