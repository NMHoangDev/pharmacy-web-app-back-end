package com.backend.inventory.repo;

import com.backend.inventory.model.InventoryItem;
import com.backend.inventory.model.InventoryItemId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

        @Query("SELECT i FROM InventoryItem i WHERE i.id.branchId = :branchId AND i.id.productId IN :productIds")
        List<InventoryItem> findByBranchIdAndProductIdIn(
                        @org.springframework.data.repository.query.Param("branchId") UUID branchId,
                        @org.springframework.data.repository.query.Param("productIds") List<UUID> productIds);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT i FROM InventoryItem i WHERE i.id.branchId = :branchId AND i.id.productId IN :productIds")
        List<InventoryItem> lockAllByBranchIdAndProductIdIn(
                        @org.springframework.data.repository.query.Param("branchId") UUID branchId,
                        @org.springframework.data.repository.query.Param("productIds") List<UUID> productIds);

        @Query("SELECT i FROM InventoryItem i WHERE i.id.branchId IN :branchIds AND i.id.productId IN :productIds")
        List<InventoryItem> findByBranchIdInAndProductIdIn(
                        @org.springframework.data.repository.query.Param("branchIds") List<UUID> branchIds,
                        @org.springframework.data.repository.query.Param("productIds") List<UUID> productIds);
}
