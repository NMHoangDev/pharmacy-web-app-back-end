package com.backend.order.service;

import com.backend.common.model.EventEnvelope;
import com.backend.order.model.OutboxEvent;
import com.backend.order.repo.OutboxEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Resolve topic name from config with a stable default.
     * This avoids compilation/runtime coupling to a potentially stale
     * common-messaging JAR.
     */
    @Value("${kafka.topics.order-events:order.events}")
    private String orderEventsTopic;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${order.outbox.publish-delay-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("NEW");
        for (OutboxEvent event : events) {
            try {
                Map<String, Object> payload = objectMapper.readValue(event.getPayload(), new TypeReference<>() {
                });
                kafkaTemplate.send(orderEventsTopic, EventEnvelope.of(event.getEventType(), "1", payload));
                event.setStatus("SENT");
            } catch (Exception ex) {
                event.setStatus("FAILED");
                log.error("Failed to publish outbox event {} type={}", event.getId(), event.getEventType(), ex);
            }
        }
    }
}