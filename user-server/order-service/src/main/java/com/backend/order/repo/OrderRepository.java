package com.backend.order.repo;

import com.backend.order.model.OrderEntity;
import com.backend.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<OrderEntity> findByUserIdAndBranchIdOrderByCreatedAtDesc(UUID userId, UUID branchId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<OrderEntity> findByStatusAndBranchIdOrderByCreatedAtDesc(OrderStatus status, UUID branchId);
}
