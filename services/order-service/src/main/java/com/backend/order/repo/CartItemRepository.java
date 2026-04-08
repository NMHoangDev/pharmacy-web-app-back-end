package com.backend.order.repo;

import com.backend.order.model.CartItem;
import com.backend.order.model.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    List<CartItem> findByIdCartUserId(UUID userId);

    void deleteByIdCartUserId(UUID userId);
}
