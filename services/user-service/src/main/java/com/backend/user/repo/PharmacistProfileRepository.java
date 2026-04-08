package com.backend.user.repo;

import com.backend.user.model.PharmacistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface PharmacistProfileRepository extends JpaRepository<PharmacistProfile, UUID> {

    @Query("SELECT p FROM PharmacistProfile p " +
            "WHERE (:status IS NULL OR p.onlineStatus = :status) " +
            "AND (:specialty IS NULL OR :specialty MEMBER OF p.specialtyTags)")
    List<PharmacistProfile> searchPharmacists(
            @Param("status") PharmacistProfile.OnlineStatus status,
            @Param("specialty") String specialty);
}
