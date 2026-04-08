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

import java.util.Map;

@Component
public class DiscountNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DiscountNotificationListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public DiscountNotificationListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Use a property placeholder (with a stable default) so this service can be
     * built
     * independently even if the local common-messaging JAR is stale.
     */
    @KafkaListener(topics = "${kafka.topics.discount-notification:discount-notification}", groupId = "notification-service")
    public void onMessage(@Payload EventEnvelope<?> event, Acknowledgment acknowledgment) {
        try {
            Map<String, Object> payload = toMap(event.payload());

            String title = stringValue(payload.get("title"));
            String message = stringValue(payload.get("message"));
            String target = stringValue(payload.get("target"));

            if (title == null || title.isBlank() || message == null || message.isBlank()) {
                acknowledgment.acknowledge();
                return;
            }

            notificationService.createBroadcastNotification(
                    "PROMOTION",
                    title,
                    message,
                    (target == null || target.isBlank()) ? "USER" : target,
                    "DISCOUNT",
                    null,
                    null);

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process discount notification event {}", event == null ? null : event.id(), ex);
        }
    }

    private Map<String, Object> toMap(Object payload) {
        return objectMapper.convertValue(payload,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
