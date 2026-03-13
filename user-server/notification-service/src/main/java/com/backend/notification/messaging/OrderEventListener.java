package com.backend.notification.messaging;

import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import com.backend.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public OrderEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TopicNames.ORDER_EVENTS, groupId = "notification-service")
    public void onMessage(@Payload EventEnvelope<?> event, Acknowledgment ack) {
        try {
            Map<String, Object> payload = toMap(event.payload());
            UUID userId = parseUuid(payload.get("userId"));
            String orderId = stringValue(payload.get("orderId"));
            String status = stringValue(payload.get("status"));
            if (userId == null || orderId == null || orderId.isBlank()) {
                log.warn("Skip order notification event id={} because payload is missing userId or orderId",
                        event.id());
                ack.acknowledge();
                return;
            }

            String title = resolveTitle(event.type());
            String message = resolveMessage(event.type(), orderId, status);
            notificationService.createUserNotification(
                    userId,
                    "ORDER",
                    title,
                    message,
                    "ORDER",
                    orderId,
                    event.type(),
                    "/orders/" + orderId);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed processing order event {}", event.id(), e);
        }
    }

    private Map<String, Object> toMap(Object payload) {
        if (payload instanceof String text) {
            try {
                return objectMapper.readValue(text, objectMapper.getTypeFactory().constructMapType(Map.class,
                        String.class, Object.class));
            } catch (Exception ignored) {
                return Map.of("raw", text);
            }
        }
        return objectMapper.convertValue(payload, objectMapper.getTypeFactory().constructMapType(Map.class,
                String.class, Object.class));
    }

    private UUID parseUuid(Object value) {
        String text = stringValue(value);
        if (text == null || text.isBlank()) {
            return null;
        }
        return UUID.fromString(text);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String resolveTitle(String eventType) {
        if (EventTypes.ORDER_PAID.equalsIgnoreCase(eventType) || "OrderPaid".equalsIgnoreCase(eventType)) {
            return "Đơn hàng của bạn đã được thanh toán";
        }
        if (EventTypes.ORDER_CANCELLED.equalsIgnoreCase(eventType) || "OrderCancelled".equalsIgnoreCase(eventType)) {
            return "Đơn hàng của bạn đã bị hủy";
        }
        return "Bạn vừa đặt đơn hàng thành công";
    }

    private String resolveMessage(String eventType, String orderId, String status) {
        String normalizedStatus = status == null || status.isBlank() ? "PENDING" : status;
        if (EventTypes.ORDER_PAID.equalsIgnoreCase(eventType) || "OrderPaid".equalsIgnoreCase(eventType)) {
            return "Đơn hàng " + orderId + " của bạn đã được xác nhận thanh toán. Trạng thái hiện tại: "
                    + normalizedStatus + ".";
        }
        if (EventTypes.ORDER_CANCELLED.equalsIgnoreCase(eventType) || "OrderCancelled".equalsIgnoreCase(eventType)) {
            return "Đơn hàng " + orderId + " của bạn đã bị hủy.";
        }
        return "Bạn vừa đặt đơn hàng " + orderId + ". Trạng thái hiện tại: " + normalizedStatus + ".";
    }
}
