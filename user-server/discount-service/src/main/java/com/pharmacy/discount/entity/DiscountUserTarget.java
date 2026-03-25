package com.pharmacy.discount.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "discount_user_targets", indexes = {
        @Index(name = "idx_discount_user_targets_discount", columnList = "discount_id"),
        @Index(name = "idx_discount_user_targets_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_discount_user_targets_discount_user", columnNames = { "discount_id", "user_id" })
})
public class DiscountUserTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

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
}
