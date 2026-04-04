package com.backend.order.repo;

import com.backend.order.model.OrderEntity;
import com.backend.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<OrderEntity> findByUserIdAndBranchIdOrderByCreatedAtDesc(UUID userId, UUID branchId);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();

    List<OrderEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    List<OrderEntity> findByStatusAndBranchIdOrderByCreatedAtDesc(OrderStatus status, UUID branchId);

    @Query("""
            select o.userId, count(o)
            from OrderEntity o
            where o.userId is not null and o.status in :statuses
            group by o.userId
            """)
    List<Object[]> countOrdersByUserIdAndStatuses(@Param("statuses") List<OrderStatus> statuses);
}
