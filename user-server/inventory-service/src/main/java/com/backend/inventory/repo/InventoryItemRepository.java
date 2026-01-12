package com.backend.inventory.repo;

import com.backend.inventory.model.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByProductIdIn(List<UUID> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.productId IN :productIds")
    List<InventoryItem> lockAllByProductIdIn(List<UUID> productIds);
}
