package com.pharmacy.discount.kafka;

import com.backend.common.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DiscountEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DiscountEventPublisher.class);

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final String discountEventsTopic;

    public DiscountEventPublisher(
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate,
            @Value("${kafka.topics.discount-events:discount.events}") String discountEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.discountEventsTopic = discountEventsTopic;
    }

    public void publishSafe(String eventType, String key, Object payload) {
        try {
            kafkaTemplate.send(discountEventsTopic, key, EventEnvelope.of(eventType, "1", payload));
        } catch (Exception ex) {
            // Resilience requirement: Kafka issues must not break APIs
            log.warn("Kafka publish skipped (topic={} type={}): {}", discountEventsTopic, eventType, ex.getMessage());
        }
    }
}
