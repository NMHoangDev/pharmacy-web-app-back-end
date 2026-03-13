package com.backend.pharmacist.repo;

import com.backend.pharmacist.model.OfflineOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OfflineOrderRepository
        extends JpaRepository<OfflineOrder, UUID>, JpaSpecificationExecutor<OfflineOrder> {
    boolean existsByOrderCode(String orderCode);
}
