package com.backend.catalog.repo;

import com.backend.catalog.model.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DrugRepository extends JpaRepository<Drug, UUID> {
        Optional<Drug> findBySlug(String slug);

        boolean existsBySlug(String slug);

        @Query("SELECT d FROM Drug d WHERE (:q IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.slug) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.sku) LIKE LOWER(CONCAT('%',:q,'%'))) "
                        + "AND (:categoryId IS NULL OR d.categoryId = :categoryId) "
                        + "AND d.status = 'ACTIVE'")
        Page<Drug> searchActive(@Param("q") String q, @Param("categoryId") UUID categoryId, Pageable pageable);

        @Query("SELECT d FROM Drug d WHERE (:q IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.slug) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.sku) LIKE LOWER(CONCAT('%',:q,'%'))) "
                        + "AND (:categoryId IS NULL OR d.categoryId = :categoryId) "
                        + "AND (:status IS NULL OR d.status = :status)")
        Page<Drug> searchAll(@Param("q") String q, @Param("categoryId") UUID categoryId, @Param("status") String status,
                        Pageable pageable);
}
