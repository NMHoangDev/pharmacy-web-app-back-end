package com.backend.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_lines")
public class ReservationLine {

    @EmbeddedId
    private ReservationLineId id;

    @Column(nullable = false)
    private int quantity;

    public ReservationLine() {
    }

    public ReservationLine(ReservationLineId id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public ReservationLineId getId() {
        return id;
    }

    public void setId(ReservationLineId id) {
        this.id = id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
