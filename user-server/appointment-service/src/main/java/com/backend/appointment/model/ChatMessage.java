package com.backend.appointment.model;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "appointment_id", nullable = false, length = 36)
    private String appointmentId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "sender_role", nullable = false, length = 16)
    private String senderRole; // USER, PHARMACIST

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 16)
    private String type = "TEXT"; // TEXT, SYSTEM

    @Column(name = "note", columnDefinition = "TEXT")
    private String note; // JSON storage for MessageNote

    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;

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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
