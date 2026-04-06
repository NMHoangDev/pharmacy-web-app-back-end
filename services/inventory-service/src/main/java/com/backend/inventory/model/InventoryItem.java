package com.backend.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @EmbeddedId
    private InventoryItemId id;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(UUID branchId, UUID productId, int onHand, int reserved) {
        this.id = new InventoryItemId(branchId, productId);
        this.onHand = onHand;
        this.reserved = reserved;
        this.updatedAt = Instant.now();
    }

    public InventoryItemId getId() {
        return id;
    }

    public void setId(InventoryItemId id) {
        this.id = id;
    }

    public UUID getProductId() {
        return id == null ? null : id.getProductId();
    }

    public UUID getBranchId() {
        return id == null ? null : id.getBranchId();
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
