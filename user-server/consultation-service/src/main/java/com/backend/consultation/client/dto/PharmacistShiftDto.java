package com.backend.consultation.client.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class PharmacistShiftDto {
    private UUID id;
    private UUID pharmacistId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String note;
    private String status;

    public PharmacistShiftDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(UUID pharmacistId) {
        this.pharmacistId = pharmacistId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
