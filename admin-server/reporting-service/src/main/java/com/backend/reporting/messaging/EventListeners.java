package com.backend.reporting.messaging;

import com.backend.reporting.service.MetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListeners {

    private final MetricService metricService;

    @KafkaListener(topics = "order.events", groupId = "reporting-service")
    public void onOrderEvent(String payload) {
        log.debug("Received order event: {}", payload);
        metricService.increment("order", "events");
    }

    @KafkaListener(topics = "review.events", groupId = "reporting-service")
    public void onReviewEvent(String payload) {
        log.debug("Received review event: {}", payload);
        metricService.increment("review", "events");
    }

    @KafkaListener(topics = "appointment.events", groupId = "reporting-service")
    public void onAppointmentEvent(String payload) {
        log.debug("Received appointment event: {}", payload);
        metricService.increment("appointment", "events");
    }
}
