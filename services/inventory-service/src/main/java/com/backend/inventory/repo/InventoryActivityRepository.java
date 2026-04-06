package com.backend.inventory.repo;

import com.backend.inventory.model.InventoryActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InventoryActivityRepository extends JpaRepository<InventoryActivity, UUID> {
    @Query("SELECT a FROM InventoryActivity a ORDER BY a.createdAt DESC")
    List<InventoryActivity> findRecent(Pageable pageable);

    @Query("SELECT a FROM InventoryActivity a WHERE a.productId = :productId ORDER BY a.createdAt DESC")
    List<InventoryActivity> findRecentByProductId(@Param("productId") UUID productId, Pageable pageable);

    @Query("SELECT a FROM InventoryActivity a WHERE a.branchId = :branchId ORDER BY a.createdAt DESC")
    List<InventoryActivity> findRecentByBranchId(@Param("branchId") UUID branchId, Pageable pageable);

    @Query("SELECT a FROM InventoryActivity a WHERE a.branchId = :branchId AND a.productId = :productId ORDER BY a.createdAt DESC")
    List<InventoryActivity> findRecentByBranchIdAndProductId(@Param("branchId") UUID branchId,
            @Param("productId") UUID productId,
            Pageable pageable);
}
