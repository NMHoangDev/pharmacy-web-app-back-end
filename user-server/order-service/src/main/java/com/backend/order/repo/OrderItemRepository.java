package com.backend.order.repo;

import com.backend.order.model.OrderItem;
import com.backend.order.model.OrderItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, OrderItemId> {
    List<OrderItem> findByIdOrderId(UUID orderId);
}
