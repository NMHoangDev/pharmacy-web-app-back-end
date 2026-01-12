package com.backend.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(columnDefinition = "json", nullable = false)
    private String payload;

    @Column(nullable = false, length = 8)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    public OutboxEvent() {
    }

    public OutboxEvent(UUID id, String eventType, String payload, String status) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
