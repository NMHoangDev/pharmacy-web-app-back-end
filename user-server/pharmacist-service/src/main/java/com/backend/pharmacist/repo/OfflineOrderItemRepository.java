package com.backend.pharmacist.repo;

import com.backend.pharmacist.model.OfflineOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfflineOrderItemRepository extends JpaRepository<OfflineOrderItem, UUID> {
    List<OfflineOrderItem> findByOrderId(UUID orderId);
}
