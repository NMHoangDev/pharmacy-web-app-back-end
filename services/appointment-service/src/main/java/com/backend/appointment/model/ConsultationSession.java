package com.backend.appointment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "consultation_sessions")
public class ConsultationSession {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "appointment_id", nullable = false, length = 36)
    private String appointmentId;

    @Column(name = "room_id", nullable = false, unique = true, length = 64)
    private String roomId;

    @Column(nullable = false, length = 16)
    private String type; // VIDEO, VOICE

    @Column(nullable = false, length = 16)
    private String status; // CREATED, RINGING, ONGOING, ENDED, MISSED, CANCELLED

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @ElementCollection
    @CollectionTable(name = "consultation_session_participants", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "user_id")
    private Set<String> participants = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "consultation_session_message_ids", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "message_id", length = 36)
    private List<String> messageIds = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<String> participants) {
        this.participants = participants;
    }

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
}
