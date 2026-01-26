package com.backend.order.repo;

import com.backend.order.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
