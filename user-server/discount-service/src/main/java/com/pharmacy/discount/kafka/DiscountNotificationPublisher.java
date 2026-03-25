package com.pharmacy.discount.kafka;

import com.backend.common.model.EventEnvelope;
import com.pharmacy.discount.entity.Discount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DiscountNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(DiscountNotificationPublisher.class);

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    /**
     * Keep the topic name stable even when the shared TopicNames JAR is
     * out-of-date.
     * (Some builds compile this module without rebuilding common-messaging first.)
     */
    private static final String DISCOUNT_NOTIFICATION_TOPIC = "discount-notification";

    public DiscountNotificationPublisher(KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishActivatedSafe(Discount discount) {
        if (discount == null || discount.getId() == null) {
            return;
        }

        String title = "Ưu đãi mới dành cho bạn";
        String message = "Chiến dịch \"" + discount.getName() + "\" đã bắt đầu. Dùng mã: " + discount.getCode();

        Map<String, Object> payload = Map.of(
                "title", title,
                "message", message,
                "type", "DISCOUNT_ACTIVATED",
                "target", "USER");

        try {
            kafkaTemplate.send(DISCOUNT_NOTIFICATION_TOPIC, String.valueOf(discount.getId()),
                    EventEnvelope.of("discount.notification", "1", payload));
        } catch (Exception ex) {
            // Resilience requirement: Kafka issues must not break APIs
            log.warn("Kafka publish skipped (topic={}): {}", DISCOUNT_NOTIFICATION_TOPIC, ex.getMessage());
        }
    }
}
