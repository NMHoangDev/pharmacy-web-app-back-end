package com.backend.notification.messaging;

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

import java.util.Locale;
import java.util.Map;

@Component
public class ContentEventListener {

    private static final Logger log = LoggerFactory.getLogger(ContentEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ContentEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TopicNames.CMS_EVENTS, groupId = "notification-service")
    public void onMessage(@Payload EventEnvelope<?> event, Acknowledgment acknowledgment) {
        try {
            if (!NotificationEventTypes.CMS_PUBLISHED.equalsIgnoreCase(event.type())) {
                acknowledgment.acknowledge();
                return;
            }

            Map<String, Object> payload = toMap(event.payload());
            String title = stringValue(payload.get("title"));
            String slug = stringValue(payload.get("slug"));
            String type = stringValue(payload.get("type"));
            String topic = stringValue(payload.get("topic"));
            boolean featured = Boolean.parseBoolean(stringValue(payload.get("featured")));

            boolean promotion = isPromotion(type, topic, title, featured);
            String category = promotion ? "PROMOTION" : "CONTENT";
            String notificationTitle = promotion ? "Ưu đãi mới dành cho bạn" : "Nội dung mới vừa được đăng";
            String notificationMessage = promotion
                    ? "Ưu đãi mới vừa được đăng: " + title
                    : "Có nội dung mới vừa được đăng: " + title;

            notificationService.createBroadcastNotification(
                    category,
                    notificationTitle,
                    notificationMessage,
                    "USER",
                    "CONTENT",
                    slug,
                    slug == null || slug.isBlank() ? null : "/content/posts/" + slug);

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process content event {}", event.id(), ex);
        }
    }

    private Map<String, Object> toMap(Object payload) {
        return objectMapper.convertValue(payload,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class,
                        Object.class));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isPromotion(String type, String topic, String title, boolean featured) {
        String haystack = ((type == null ? "" : type) + " "
                + (topic == null ? "" : topic) + " "
                + (title == null ? "" : title)).toLowerCase(Locale.ROOT);
        return featured || haystack.contains("promo") || haystack.contains("offer")
                || haystack.contains("khuyen") || haystack.contains("ưu đãi") || haystack.contains("uu dai");
    }
}