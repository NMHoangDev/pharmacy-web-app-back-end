package com.backend.order.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "char(36)")
    private UUID userId;

    @Column(name = "order_code", length = 64)
    private String orderCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "branch_id", columnDefinition = "char(36)")
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fulfillment_branch_id", columnDefinition = "char(36)")
    private UUID fulfillmentBranchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fulfillment_assigned_by", columnDefinition = "char(36)")
    private UUID fulfillmentAssignedBy;

    @Column(name = "fulfillment_assigned_at")
    private Instant fulfillmentAssignedAt;

    @Column(name = "fulfillment_status", length = 32)
    private String fulfillmentStatus;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "inventory_reservation_id", columnDefinition = "char(36)")
    private UUID inventoryReservationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "subtotal", nullable = false)
    private double subtotal;

    @Column(name = "shipping_fee", nullable = false)
    private double shippingFee;

    @Column(name = "discount_amount", nullable = false)
    private double discountAmount;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount; // Grand total

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "payment_status", length = 32)
    private String paymentStatus;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipping_address_id")
    private OrderShippingAddress shippingAddress;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shipping_id")
    private OrderShipping shipping;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "discount_id")
    private OrderDiscount discount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public OrderEntity() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getFulfillmentBranchId() {
        return fulfillmentBranchId;
    }

    public void setFulfillmentBranchId(UUID fulfillmentBranchId) {
        this.fulfillmentBranchId = fulfillmentBranchId;
    }

    public UUID getFulfillmentAssignedBy() {
        return fulfillmentAssignedBy;
    }

    public void setFulfillmentAssignedBy(UUID fulfillmentAssignedBy) {
        this.fulfillmentAssignedBy = fulfillmentAssignedBy;
    }

    public Instant getFulfillmentAssignedAt() {
        return fulfillmentAssignedAt;
    }

    public void setFulfillmentAssignedAt(Instant fulfillmentAssignedAt) {
        this.fulfillmentAssignedAt = fulfillmentAssignedAt;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public UUID getInventoryReservationId() {
        return inventoryReservationId;
    }

    public void setInventoryReservationId(UUID inventoryReservationId) {
        this.inventoryReservationId = inventoryReservationId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OrderShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(OrderShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public OrderShipping getShipping() {
        return shipping;
    }

    public void setShipping(OrderShipping shipping) {
        this.shipping = shipping;
    }

    public OrderDiscount getDiscount() {
        return discount;
    }

    public void setDiscount(OrderDiscount discount) {
        this.discount = discount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
