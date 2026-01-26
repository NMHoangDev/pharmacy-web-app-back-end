package com.backend.appointment.model;

public enum AppointmentStatus {
    // old values kept for backward compatibility
    REQUESTED,
    DONE,

    // preferred new lifecycle
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
