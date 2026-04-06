package com.backend.order.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.UUID;

@Entity
@Table(name = "order_shipping")
public class OrderShipping {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(nullable = false)
    private String method; // STANDARD, EXPRESS_2H

    @Column(nullable = false)
    private double fee;

    @Column(name = "eta_range")
    private String etaRange;

    public OrderShipping() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getEtaRange() {
        return etaRange;
    }

    public void setEtaRange(String etaRange) {
        this.etaRange = etaRange;
    }
}
