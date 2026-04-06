package com.backend.pharmacist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offline_orders", indexes = {
        @Index(name = "idx_offline_order_branch", columnList = "branch_id"),
        @Index(name = "idx_offline_order_pharmacist", columnList = "pharmacist_id"),
        @Index(name = "idx_offline_order_status", columnList = "status"),
        @Index(name = "idx_offline_order_created", columnList = "created_at")
})
public class OfflineOrder {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "order_code", nullable = false, unique = true, length = 32)
    private String orderCode;

    @Column(name = "order_type", nullable = false, length = 16)
    private String orderType = "OFFLINE";

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "branch_id", nullable = false, columnDefinition = "char(36)")
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pharmacist_id", nullable = false, columnDefinition = "char(36)")
    private UUID pharmacistId;

    @Column(name = "customer_name", length = 128)
    private String customerName;

    @Column(name = "customer_phone", length = 32)
    private String customerPhone;

    @Column(name = "consultation_id", length = 64)
    private String consultationId;

    @Column(name = "note", length = 512)
    private String note;

    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    @Column(name = "discount", nullable = false)
    private Long discount = 0L;

    @Column(name = "tax_fee", nullable = false)
    private Long taxFee = 0L;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OfflineOrderStatus status = OfflineOrderStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 24)
    private OfflinePaymentMethod paymentMethod;

    @Column(name = "amount_received")
    private Long amountReceived;

    @Column(name = "change_amount")
    private Long changeAmount;

    @Column(name = "transfer_reference", length = 128)
    private String transferReference;

    @Column(name = "payment_proof_url", columnDefinition = "TEXT")
    private String paymentProofUrl;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "inventory_reservation_id", columnDefinition = "char(36)")
    private UUID inventoryReservationId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(UUID pharmacistId) {
        this.pharmacistId = pharmacistId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(String consultationId) {
        this.consultationId = consultationId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Long subtotal) {
        this.subtotal = subtotal;
    }

    public Long getDiscount() {
        return discount;
    }

    public void setDiscount(Long discount) {
        this.discount = discount;
    }

    public Long getTaxFee() {
        return taxFee;
    }

    public void setTaxFee(Long taxFee) {
        this.taxFee = taxFee;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OfflineOrderStatus getStatus() {
        return status;
    }

    public void setStatus(OfflineOrderStatus status) {
        this.status = status;
    }

    public OfflinePaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(OfflinePaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(Long amountReceived) {
        this.amountReceived = amountReceived;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setTransferReference(String transferReference) {
        this.transferReference = transferReference;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }

    public UUID getInventoryReservationId() {
        return inventoryReservationId;
    }

    public void setInventoryReservationId(UUID inventoryReservationId) {
        this.inventoryReservationId = inventoryReservationId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
