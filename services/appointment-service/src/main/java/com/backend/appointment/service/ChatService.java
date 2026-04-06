package com.backend.appointment.service;

import com.backend.appointment.api.dto.MessageRequest;
import com.backend.appointment.api.dto.MessageResponse;
import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.ChatMessage;
import com.backend.appointment.repo.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.backend.appointment.api.dto.MessageNote;
import com.backend.appointment.repo.ConsultationSessionRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ConsultationSessionRepository consultationSessionRepository;
    private final AppointmentAccessService appointmentAccessService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public ChatService(ChatMessageRepository chatMessageRepository,
            ConsultationSessionRepository consultationSessionRepository,
            AppointmentAccessService appointmentAccessService,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.consultationSessionRepository = consultationSessionRepository;
        this.appointmentAccessService = appointmentAccessService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public List<MessageResponse> getHistory(String appointmentId, String userId, int limit) {
        // Validate access (user must be participant)
        appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);

        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId,
                pageable);

        // Return in chronological order (oldest first) if needed by FE, but usually
        // desc for fetch.
        // Let's reverse them to be chronological for current view or keep as is.
        // Usually FE prepends. Let's keep DESC but doc says "Chat history".

        return messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public MessageResponse saveAndBroadcast(String appointmentId, String userId, MessageRequest request) {
        Appointment appointment = appointmentAccessService.getAppointmentIfAuthorized(appointmentId, userId);

        UUID senderId = UUID.fromString(userId);
        String role = senderId.equals(appointment.getUserId()) ? "USER" : "PHARMACIST";

        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setAppointmentId(appointmentId);
        message.setSenderId(userId);
        message.setSenderRole(role);
        message.setContent(request.getContent());
        message.setType("TEXT");

        if (request.getNote() != null) {
            try {
                message.setNote(objectMapper.writeValueAsString(request.getNote()));
            } catch (Exception e) {
                // Ignore serialization error or log it
            }
        }

        message.setCreatedAt(Timestamp.from(Instant.now()));

        ChatMessage saved = chatMessageRepository.save(message);
        trackMessageIdInLatestSession(appointmentId, saved.getId());
        MessageResponse response = toResponse(saved);

        // Broadcast to /topic/appointments/{appointmentId}/chat
        messagingTemplate.convertAndSend("/topic/appointments/" + appointmentId + "/chat", response);

        return response;
    }

    public long purgeSessionMessages(String appointmentId, String actorId, List<String> messageIds) {
        appointmentAccessService.getAppointmentIfAuthorized(appointmentId, actorId);

        if (messageIds == null || messageIds.isEmpty()) {
            return 0L;
        }

        LinkedHashSet<String> uniqueIds = messageIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueIds.isEmpty()) {
            return 0L;
        }

        return chatMessageRepository.deleteByAppointmentIdAndIdIn(appointmentId, uniqueIds);
    }

    private void trackMessageIdInLatestSession(String appointmentId, String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }

        consultationSessionRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .ifPresent(session -> {
                    List<String> ids = session.getMessageIds();
                    if (ids == null) {
                        ids = new ArrayList<>();
                        session.setMessageIds(ids);
                    }
                    if (!ids.contains(messageId)) {
                        ids.add(messageId);
                        consultationSessionRepository.save(session);
                    }
                });
    }

    private MessageResponse toResponse(ChatMessage entity) {
        MessageResponse dto = new MessageResponse();
        dto.setId(entity.getId());
        dto.setSenderId(entity.getSenderId());
        dto.setSenderRole(entity.getSenderRole());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType());

        if (entity.getNote() != null) {
            try {
                dto.setNote(objectMapper.readValue(entity.getNote(), MessageNote.class));
            } catch (Exception e) {
                // Ignore deserialization error
            }
        }

        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
