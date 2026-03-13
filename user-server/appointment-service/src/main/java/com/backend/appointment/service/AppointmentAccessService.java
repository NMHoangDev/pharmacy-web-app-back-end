package com.backend.appointment.service;

import com.backend.appointment.model.Appointment;
import com.backend.appointment.repo.AppointmentRepository;
import com.backend.appointment.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppointmentAccessService {

    private final AppointmentRepository appointmentRepository;

    // Configurable windows (hardcoded for MVP as per request)
    private static final int JOIN_WINDOW_BEFORE_MINUTES = 10;
    private static final int JOIN_WINDOW_AFTER_MINUTES = 30;

    public AppointmentAccessService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment getAppointmentIfAuthorized(String appointmentId, String userId) {
        UUID appUuid = UUID.fromString(appointmentId);
        Appointment appointment = appointmentRepository.findById(appUuid)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Admin can access any appointment (observer role)
        if (SecurityUtils.isAdmin()) {
            return appointment;
        }

        UUID userUuid = UUID.fromString(userId);
        if (!appointment.getUserId().equals(userUuid) && !appointment.getPharmacistId().equals(userUuid)) {
            throw new SecurityException("User is not a participant of this appointment");
        }
        return appointment;
    }

    public Appointment requireParticipant(UUID appointmentId, UUID actorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.getUserId().equals(actorId) && !appointment.getPharmacistId().equals(actorId)) {
            throw new SecurityException("User is not a participant of this appointment");
        }
        return appointment;
    }

    public Appointment requirePharmacist(UUID appointmentId, UUID actorId) {
        if (actorId == null) {
            throw new SecurityException("Unable to resolve pharmacist identity");
        }
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.getPharmacistId().equals(actorId)) {
            throw new SecurityException("Only assigned pharmacist can access this appointment");
        }
        return appointment;
    }

    public void validateCallWindow(Appointment appointment) {
        // Admin can join any consultation room at any time for monitoring
        if (SecurityUtils.isAdmin()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validStart = appointment.getStartAt().minusMinutes(JOIN_WINDOW_BEFORE_MINUTES);
        LocalDateTime validEnd = appointment.getEndAt().plusMinutes(JOIN_WINDOW_AFTER_MINUTES);

        if (now.isBefore(validStart)) {
            throw new IllegalStateException("Appointment has not started yet (window opens 10 mins before).");
        }
        if (now.isAfter(validEnd)) {
            throw new IllegalStateException("Appointment window has expired.");
        }
    }
}
