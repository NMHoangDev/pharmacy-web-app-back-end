package com.backend.appointment.model;

public enum AppointmentStatus {
    // legacy values kept for backward compatibility
    REQUESTED,
    PENDING,
    DONE,

    // preferred lifecycle
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    RESCHEDULED,
    REFUNDED
}
