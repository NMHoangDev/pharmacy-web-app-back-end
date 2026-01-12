package com.backend.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @Column(name = "product_id", columnDefinition = "char(36)")
    private UUID productId;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(UUID productId, int onHand, int reserved) {
        this.productId = productId;
        this.onHand = onHand;
        this.reserved = reserved;
        this.updatedAt = Instant.now();
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getOnHand() {
        return onHand;
    }

    public void setOnHand(int onHand) {
        this.onHand = onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
