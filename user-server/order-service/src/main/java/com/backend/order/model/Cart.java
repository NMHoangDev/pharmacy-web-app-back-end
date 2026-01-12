package com.backend.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @Column(name = "user_id", columnDefinition = "char(36)")
    private UUID userId;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Cart() {
    }

    public Cart(UUID userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
