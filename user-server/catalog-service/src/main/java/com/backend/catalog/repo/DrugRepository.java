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

        @Query("SELECT d.id as id, d.sku as sku, d.name as name, d.slug as slug, d.categoryId as categoryId, "
                        + "d.costPrice as costPrice, d.salePrice as baseSalePrice, d.status as globalStatus, d.prescriptionRequired as prescriptionRequired, "
                        + "d.description as description, d.dosageForm as dosageForm, d.packaging as packaging, "
                        + "d.activeIngredient as activeIngredient, d.indications as indications, d.usageDosage as usageDosage, "
                        + "d.contraindicationsWarning as contraindicationsWarning, d.otherInformation as otherInformation, "
                        + "d.imageUrl as imageUrl, d.attributes as attributes, "
                        + "s.priceOverride as priceOverride, s.status as branchStatus, s.note as note "
                        + "FROM Drug d LEFT JOIN DrugBranchSetting s ON s.drugId = d.id AND s.branchId = :branchId "
                        + "WHERE (:q IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.slug) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.sku) LIKE LOWER(CONCAT('%',:q,'%'))) "
                        + "AND (:categoryId IS NULL OR d.categoryId = :categoryId) "
                        + "AND d.status = 'ACTIVE' "
                        + "AND (s.status IS NULL OR s.status = 'ACTIVE')")
        Page<DrugWithBranchView> searchPublicByBranch(@Param("q") String q, @Param("categoryId") UUID categoryId,
                        @Param("branchId") UUID branchId, Pageable pageable);

        @Query("SELECT d.id as id, d.sku as sku, d.name as name, d.slug as slug, d.categoryId as categoryId, "
                        + "d.costPrice as costPrice, d.salePrice as baseSalePrice, d.status as globalStatus, d.prescriptionRequired as prescriptionRequired, "
                        + "d.description as description, d.dosageForm as dosageForm, d.packaging as packaging, "
                        + "d.activeIngredient as activeIngredient, d.indications as indications, d.usageDosage as usageDosage, "
                        + "d.contraindicationsWarning as contraindicationsWarning, d.otherInformation as otherInformation, "
                        + "d.imageUrl as imageUrl, d.attributes as attributes, "
                        + "s.priceOverride as priceOverride, s.status as branchStatus, s.note as note "
                        + "FROM Drug d LEFT JOIN DrugBranchSetting s ON s.drugId = d.id AND s.branchId = :branchId "
                        + "WHERE (:q IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.slug) LIKE LOWER(CONCAT('%',:q,'%')) "
                        + "OR LOWER(d.sku) LIKE LOWER(CONCAT('%',:q,'%'))) "
                        + "AND (:categoryId IS NULL OR d.categoryId = :categoryId) "
                        + "AND (:status IS NULL OR d.status = :status)")
        Page<DrugWithBranchView> searchAdminByBranch(@Param("q") String q, @Param("categoryId") UUID categoryId,
                        @Param("status") String status, @Param("branchId") UUID branchId, Pageable pageable);

        @Query("SELECT d.id as id, d.sku as sku, d.name as name, d.slug as slug, d.categoryId as categoryId, "
                        + "d.costPrice as costPrice, d.salePrice as baseSalePrice, d.status as globalStatus, d.prescriptionRequired as prescriptionRequired, "
                        + "d.description as description, d.dosageForm as dosageForm, d.packaging as packaging, "
                        + "d.activeIngredient as activeIngredient, d.indications as indications, d.usageDosage as usageDosage, "
                        + "d.contraindicationsWarning as contraindicationsWarning, d.otherInformation as otherInformation, "
                        + "d.imageUrl as imageUrl, d.attributes as attributes, "
                        + "s.priceOverride as priceOverride, s.status as branchStatus, s.note as note "
                        + "FROM Drug d LEFT JOIN DrugBranchSetting s ON s.drugId = d.id AND s.branchId = :branchId "
                        + "WHERE d.id = :drugId")
        Optional<DrugWithBranchView> findWithBranchById(@Param("drugId") UUID drugId,
                        @Param("branchId") UUID branchId);

        @Query("SELECT d.id as id, d.sku as sku, d.name as name, d.slug as slug, d.categoryId as categoryId, "
                        + "d.costPrice as costPrice, d.salePrice as baseSalePrice, d.status as globalStatus, d.prescriptionRequired as prescriptionRequired, "
                        + "d.description as description, d.dosageForm as dosageForm, d.packaging as packaging, "
                        + "d.activeIngredient as activeIngredient, d.indications as indications, d.usageDosage as usageDosage, "
                        + "d.contraindicationsWarning as contraindicationsWarning, d.otherInformation as otherInformation, "
                        + "d.imageUrl as imageUrl, d.attributes as attributes, "
                        + "s.priceOverride as priceOverride, s.status as branchStatus, s.note as note "
                        + "FROM Drug d LEFT JOIN DrugBranchSetting s ON s.drugId = d.id AND s.branchId = :branchId "
                        + "WHERE d.slug = :slug")
        Optional<DrugWithBranchView> findWithBranchBySlug(@Param("slug") String slug,
                        @Param("branchId") UUID branchId);
}
