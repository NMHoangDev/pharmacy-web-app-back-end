package com.backend.appointment.messaging;

public final class AppointmentEventTypes {

    public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
    public static final String APPOINTMENT_ACCEPTED = "APPOINTMENT_ACCEPTED";
    public static final String APPOINTMENT_REJECTED = "APPOINTMENT_REJECTED";
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    public static final String APPOINTMENT_STATUS_UPDATED = "APPOINTMENT_STATUS_UPDATED";

    private AppointmentEventTypes() {
    }
}
