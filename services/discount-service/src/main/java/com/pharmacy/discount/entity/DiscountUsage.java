package com.pharmacy.discount.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "discount_usages", indexes = {
        @Index(name = "idx_discount_usages_discount", columnList = "discount_id"),
        @Index(name = "idx_discount_usages_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_discount_usages_discount_user_order", columnNames = { "discount_id", "user_id",
                "order_id" })
})
public class DiscountUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @PrePersist
    void prePersist() {
        if (usedAt == null) {
            usedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Discount getDiscount() {
        return discount;
    }

    public void setDiscount(Discount discount) {
        this.discount = discount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }
}
