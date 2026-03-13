package com.backend.pharmacist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offline_order_payments", indexes = {
        @Index(name = "idx_offline_payment_order", columnList = "order_id"),
        @Index(name = "idx_offline_payment_created", columnList = "created_at")
})
public class OfflineOrderPayment {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "order_id", nullable = false, columnDefinition = "char(36)")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private OfflinePaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 24)
    private OfflinePaymentMethod method;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "amount_received")
    private Long amountReceived;

    @Column(name = "change_amount")
    private Long changeAmount;

    @Column(name = "transfer_reference", length = 128)
    private String transferReference;

    @Column(name = "proof_url", columnDefinition = "TEXT")
    private String proofUrl;

    @Column(name = "note", length = 512)
    private String note;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", nullable = false, columnDefinition = "char(36)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public OfflinePaymentEventType getEventType() {
        return eventType;
    }

    public void setEventType(OfflinePaymentEventType eventType) {
        this.eventType = eventType;
    }

    public OfflinePaymentMethod getMethod() {
        return method;
    }

    public void setMethod(OfflinePaymentMethod method) {
        this.method = method;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
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

    public String getProofUrl() {
        return proofUrl;
    }

    public void setProofUrl(String proofUrl) {
        this.proofUrl = proofUrl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
