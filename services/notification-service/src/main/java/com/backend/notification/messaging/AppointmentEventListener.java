package com.backend.notification.messaging;

import com.backend.common.model.EventEnvelope;
import com.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);
    private static final DateTimeFormatter APPOINTMENT_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public AppointmentEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "appointment.events", groupId = "notification-service")
    public void onMessage(@Payload EventEnvelope<?> event, Acknowledgment acknowledgment) {
        try {
            Map<String, Object> payload = toMap(event.payload());
            UUID userId = parseUuid(payload.get("userId"));
            UUID appointmentId = parseUuid(payload.get("id"));
            if (userId == null || appointmentId == null) {
                log.warn("Skip appointment notification event id={} because payload is missing userId or id",
                        event.id());
                acknowledgment.acknowledge();
                return;
            }

            String status = stringValue(payload.get("status"));
            String statusLabel = mapAppointmentStatus(status);
            LocalDateTime startAt = parseDateTime(payload.get("startAt"));
            String scheduledAt = startAt == null ? "chưa xác định" : APPOINTMENT_TIME_FORMAT.format(startAt);

            String title = resolveTitle(event.type());
            String message = resolveMessage(event.type(), statusLabel, scheduledAt);

            notificationService.createUserNotification(
                    userId,
                    "APPOINTMENT",
                    title,
                    message,
                    "APPOINTMENT",
                    appointmentId.toString(),
                    event.type(),
                    "/account");

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process appointment event {}", event.id(), ex);
        }
    }

    private Map<String, Object> toMap(Object payload) {
        return objectMapper.convertValue(payload,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class,
                        Object.class));
    }

    private UUID parseUuid(Object value) {
        String text = stringValue(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        return UUID.fromString(text);
    }

    private LocalDateTime parseDateTime(Object value) {
        String text = stringValue(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String mapAppointmentStatus(String status) {
        if (status == null) {
            return "có cập nhật mới";
        }
        return switch (status.toUpperCase()) {
            case "REQUESTED", "PENDING" -> "đang chờ dược sĩ chấp nhận";
            case "CONFIRMED" -> "được chấp nhận";
            case "RESCHEDULED" -> "đã được đổi lịch";
            case "CANCELLED" -> "đã bị hủy";
            case "IN_PROGRESS" -> "đang diễn ra";
            case "COMPLETED" -> "đã hoàn thành";
            default -> "có cập nhật mới";
        };
    }

    private String resolveTitle(String eventType) {
        if (NotificationEventTypes.APPOINTMENT_CREATED.equalsIgnoreCase(eventType)) {
            return "Đặt lịch tư vấn thành công";
        }
        if (NotificationEventTypes.APPOINTMENT_ACCEPTED.equalsIgnoreCase(eventType)
                || NotificationEventTypes.APPOINTMENT_CONFIRMED.equalsIgnoreCase(eventType)) {
            return "Lịch hẹn đã được chấp nhận";
        }
        if (NotificationEventTypes.APPOINTMENT_REJECTED.equalsIgnoreCase(eventType)) {
            return "Lịch hẹn bị từ chối";
        }
        if (NotificationEventTypes.APPOINTMENT_CANCELLED.equalsIgnoreCase(eventType)) {
            return "Lịch hẹn đã bị hủy";
        }
        return "Bạn có cập nhật lịch tư vấn";
    }

    private String resolveMessage(String eventType, String statusLabel, String scheduledAt) {
        if (NotificationEventTypes.APPOINTMENT_CREATED.equalsIgnoreCase(eventType)) {
            return "Lịch tư vấn của bạn đã được ghi nhận và đang chờ xác nhận. Thời gian dự kiến: " + scheduledAt
                    + ".";
        }
        if (NotificationEventTypes.APPOINTMENT_ACCEPTED.equalsIgnoreCase(eventType)
                || NotificationEventTypes.APPOINTMENT_CONFIRMED.equalsIgnoreCase(eventType)) {
            return "Dược sĩ đã chấp nhận lịch tư vấn của bạn. Thời gian: " + scheduledAt + ".";
        }
        if (NotificationEventTypes.APPOINTMENT_REJECTED.equalsIgnoreCase(eventType)) {
            return "Dược sĩ đã từ chối lịch tư vấn của bạn.";
        }
        if (NotificationEventTypes.APPOINTMENT_CANCELLED.equalsIgnoreCase(eventType)) {
            return "Lịch tư vấn của bạn đã bị hủy.";
        }
        return "Lịch tư vấn của bạn vừa có cập nhật mới: " + statusLabel + ".";
    }
}