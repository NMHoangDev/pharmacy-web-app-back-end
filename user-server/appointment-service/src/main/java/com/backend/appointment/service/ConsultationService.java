package com.backend.appointment.service;

import com.backend.appointment.api.dto.ConsultationRequest;
import com.backend.appointment.api.dto.ConsultationResponse;
import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.ConsultationSession;
import com.backend.appointment.repo.ConsultationSessionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsultationService {

    private final ConsultationSessionRepository sessionRepository;
    private final AppointmentAccessService appointmentAccessService;
    private final AppointmentAuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    public ConsultationService(ConsultationSessionRepository sessionRepository,
            AppointmentAccessService appointmentAccessService,
            AppointmentAuditService auditService,
            SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.appointmentAccessService = appointmentAccessService;
        this.auditService = auditService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ConsultationResponse createSession(String appointmentId, String userId, ConsultationRequest request) {
        Appointment appointment = appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);
        appointmentAccessService.validateCallWindow(appointment);

        ConsultationSession session = new ConsultationSession();
        session.setId(UUID.randomUUID().toString());
        session.setAppointmentId(appointmentId);
        session.setRoomId(UUID.randomUUID().toString());
        session.setType(request.getType());
        session.setStatus("CREATED");
        session.setCreatedBy(userId);
        session.setExpiresAt(LocalDateTime.now().plusHours(1)); // Default 1h exp

        session = sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public ConsultationResponse joinSession(String roomId, String userId) {
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        appointmentAccessService.getAppointmentIfAuthorized(session.getAppointmentId(), userId);

        if ("ENDED".equals(session.getStatus())) {
            throw new IllegalStateException("Consultation has already ended");
        }

        session.getParticipants().add(userId);

        if ("CREATED".equals(session.getStatus()) || "RINGING".equals(session.getStatus())) {
            session.setStatus("ACTIVE");
            session.setStartedAt(LocalDateTime.now());
        }

        session = sessionRepository.save(session);
        broadcastEvent(session.getAppointmentId(), "JOINED", session, userId);
        return toResponse(session);
    }

    @Transactional
    public ConsultationResponse leaveSession(String roomId, String userId) {
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        session.getParticipants().remove(userId);

        if (session.getParticipants().isEmpty() && "ACTIVE".equals(session.getStatus())) {
            session.setStatus("ENDED");
            session.setEndedAt(LocalDateTime.now());
        }

        session = sessionRepository.save(session);
        broadcastEvent(session.getAppointmentId(), "LEFT", session, userId);
        return toResponse(session);
    }

    public ConsultationResponse getLatestSession(String appointmentId, String userId) {
        appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);
        return sessionRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public void ring(String sessionId, String userId) {
        ConsultationSession session = getSessionOrThrow(sessionId);
        appointmentAccessService.getAppointmentIfAuthorized(session.getAppointmentId(), userId);

        if (!"CREATED".equals(session.getStatus()) && !"RINGING".equals(session.getStatus())) {
            // Allow RINGING to be called multiple times if needed
        }

        session.setStatus("RINGING");
        sessionRepository.save(session);
        broadcastEvent(session.getAppointmentId(), "RINGING", session, userId);
    }

    @Transactional
    public void updateNotes(String appointmentId, String userId, String notes) {
        Appointment appointment = appointmentAccessService.requirePharmacist(UUID.fromString(appointmentId),
                UUID.fromString(userId));
        appointment.setNotes(notes);
        auditService.log(appointment.getId(), "NOTES_UPDATE", null, appointment.getStatus().name(), null,
                "PHARMACIST", null, null);
        // appointmentRepository.save(appointment); // No need if transactional
    }

    private ConsultationSession getSessionOrThrow(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    private void broadcastEvent(String appointmentId, String eventType, ConsultationSession session, String userId) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("event", eventType);
        payload.put("userId", userId);
        payload.put("sessionId", session.getId());
        payload.put("roomId", session.getRoomId());
        payload.put("status", session.getStatus());
        payload.put("type", session.getType());
        payload.put("participants", session.getParticipants());

        messagingTemplate.convertAndSend("/topic/appointments/" + appointmentId + "/call", payload);
    }

    private ConsultationResponse toResponse(ConsultationSession entity) {
        ConsultationResponse dto = new ConsultationResponse();
        dto.setId(entity.getId());
        dto.setAppointmentId(entity.getAppointmentId());
        dto.setRoomId(entity.getRoomId());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setParticipants(entity.getParticipants());
        return dto;
    }
}
