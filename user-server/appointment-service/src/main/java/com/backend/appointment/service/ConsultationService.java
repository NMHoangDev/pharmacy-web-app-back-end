package com.backend.appointment.service;

import com.backend.appointment.api.dto.ConsultationPrescriptionOrderRequest;
import com.backend.appointment.api.dto.ConsultationPrescriptionOrderResponse;
import com.backend.appointment.api.dto.ConsultationPrescriptionProductPageResponse;
import com.backend.appointment.api.dto.ConsultationPrescriptionProductResponse;
import com.backend.appointment.api.dto.ConsultationRequest;
import com.backend.appointment.api.dto.ConsultationResponse;
import com.backend.appointment.client.PharmacistClient;
import com.backend.appointment.client.dto.pos.PosCreateOfflineOrderRequestDto;
import com.backend.appointment.client.dto.pos.PosOfflineOrderResponseDto;
import com.backend.appointment.client.dto.pos.PosOrderItemRequestDto;
import com.backend.appointment.client.dto.pos.PosProductSearchPageDto;
import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.ConsultationSession;
import com.backend.appointment.repo.ConsultationSessionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConsultationService {

    private final ConsultationSessionRepository sessionRepository;
    private final AppointmentAccessService appointmentAccessService;
    private final PharmacistClient pharmacistClient;
    private final AppointmentAuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    public ConsultationService(ConsultationSessionRepository sessionRepository,
            AppointmentAccessService appointmentAccessService,
            PharmacistClient pharmacistClient,
            AppointmentAuditService auditService,
            SimpMessagingTemplate messagingTemplate,
            ChatService chatService) {
        this.sessionRepository = sessionRepository;
        this.appointmentAccessService = appointmentAccessService;
        this.pharmacistClient = pharmacistClient;
        this.auditService = auditService;
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @Transactional
    public ConsultationResponse createSession(String appointmentId, String userId, ConsultationRequest request) {
        Appointment appointment = appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);

        // Return the existing active session so all participants share the same roomId
        Optional<ConsultationSession> existing = sessionRepository
                .findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId);
        if (existing.isPresent() && !"ENDED".equals(existing.get().getStatus())) {
            return toResponse(existing.get());
        }

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

    @Transactional
    public ConsultationResponse endSession(String roomId, String userId, List<String> clientMessageIds) {
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        appointmentAccessService.getAppointmentIfAuthorized(session.getAppointmentId(), userId);

        if (!"ENDED".equals(session.getStatus())) {
            session.setStatus("ENDED");
            session.setEndedAt(LocalDateTime.now());
        }
        session.getParticipants().clear();

        LinkedHashSet<String> idsToDelete = new LinkedHashSet<>();
        if (session.getMessageIds() != null) {
            idsToDelete.addAll(session.getMessageIds());
        }
        if (clientMessageIds != null) {
            idsToDelete.addAll(clientMessageIds);
        }

        if (!idsToDelete.isEmpty()) {
            chatService.purgeSessionMessages(session.getAppointmentId(), userId, new ArrayList<>(idsToDelete));
        }

        session.setMessageIds(new ArrayList<>());
        session = sessionRepository.save(session);
        broadcastEvent(session.getAppointmentId(), "ENDED", session, userId);
        return toResponse(session);
    }

    public ConsultationResponse getLatestSession(String appointmentId, String userId) {
        appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);
        return sessionRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .map(this::toResponse)
                .orElse(null);
    }

    public ConsultationResponse getSessionDetails(String roomId, String userId) {
        ConsultationSession session = sessionRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        appointmentAccessService.getAppointmentIfAuthorized(session.getAppointmentId(), userId);
        return toResponse(session);
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

    public ConsultationPrescriptionProductPageResponse searchPrescriptionProducts(String appointmentId, String userId,
            String query,
            int page, int size) {
        Appointment appointment = appointmentAccessService.requirePharmacist(
                UUID.fromString(appointmentId),
                UUID.fromString(userId));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PosProductSearchPageDto source = pharmacistClient.searchPosProducts(query, appointment.getBranchId(), safePage,
                safeSize);

        var content = source.content() == null
                ? java.util.List.<ConsultationPrescriptionProductResponse>of()
                : source.content().stream()
                        .map(item -> new ConsultationPrescriptionProductResponse(item.productId(), item.name()))
                        .toList();

        return new ConsultationPrescriptionProductPageResponse(
                content,
                source.totalElements(),
                source.page(),
                source.size(),
                source.totalPages());
    }

    public ConsultationPrescriptionOrderResponse createPrescriptionOrder(String appointmentId, String userId,
            ConsultationPrescriptionOrderRequest request) {
        Appointment appointment = appointmentAccessService.requirePharmacist(
                UUID.fromString(appointmentId),
                UUID.fromString(userId));

        if (appointment.getBranchId() == null) {
            throw new IllegalArgumentException("Appointment is missing branchId");
        }
        if (appointment.getPharmacistId() == null) {
            throw new IllegalArgumentException("Appointment is missing pharmacistId");
        }

        var sortedItems = new ArrayList<>(request.items());
        sortedItems.sort(Comparator.comparing(item -> String.valueOf(item.productId())));

        var orderItems = sortedItems.stream()
                .map(item -> new PosOrderItemRequestDto(
                        item.productId(),
                        trimToNull(item.sku()),
                        firstNonBlank(item.name(), item.sku()),
                        trimToNull(item.batchNo()),
                        trimToNull(item.expiryDate()),
                        Math.max(item.quantity() == null ? 1 : item.quantity(), 1),
                        resolveUnitPrice(item.productId(), item.name(), appointment.getBranchId(), item.unitPrice())))
                .toList();

        String mergedNote = mergeNotes(request.note(), request.prescriptionTitle(), request.prescriptionSummary());
        PosCreateOfflineOrderRequestDto payload = new PosCreateOfflineOrderRequestDto(
                appointment.getBranchId(),
                appointment.getPharmacistId(),
                orderItems,
                Math.max(request.discount() == null ? 0L : request.discount(), 0L),
                Math.max(request.taxFee() == null ? 0L : request.taxFee(), 0L),
                firstNonBlank(request.customerName(), appointment.getFullName()),
                firstNonBlank(request.customerPhone(), appointment.getContact()),
                mergedNote,
                appointmentId);

        PosOfflineOrderResponseDto created = pharmacistClient.createPosOrder(payload);
        return new ConsultationPrescriptionOrderResponse(
                created.id(),
                created.orderCode(),
                appointmentId,
                created.totalAmount(),
                created.status());
    }

    private String mergeNotes(String note, String title, String summary) {
        String base = trimToNull(note);
        String normalizedTitle = trimToNull(title);
        String normalizedSummary = trimToNull(summary);
        String summaryBlock = normalizedSummary == null
                ? null
                : (normalizedTitle == null ? normalizedSummary : (normalizedTitle + ": " + normalizedSummary));

        if (base == null) {
            return summaryBlock;
        }
        if (summaryBlock == null) {
            return base;
        }
        return base + " | " + summaryBlock;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = trimToNull(primary);
        return first != null ? first : trimToNull(fallback);
    }

    private Long resolveUnitPrice(UUID productId, String productName, UUID branchId, Long requestedUnitPrice) {
        if (requestedUnitPrice != null && requestedUnitPrice > 0) {
            return requestedUnitPrice;
        }

        String query = trimToNull(productName);
        if (query == null) {
            return 0L;
        }

        try {
            PosProductSearchPageDto page = pharmacistClient.searchPosProducts(query, branchId, 0, 20);
            if (page == null || page.content() == null) {
                return 0L;
            }

            return page.content().stream()
                    .filter(item -> productId != null && productId.equals(item.productId()))
                    .findFirst()
                    .map(item -> item.unitPrice() == null ? 0L : Math.max(item.unitPrice(), 0L))
                    .orElse(0L);
        } catch (Exception ignored) {
            return 0L;
        }
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
