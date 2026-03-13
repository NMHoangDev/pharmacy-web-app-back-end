package com.backend.payment.service;

import com.backend.payment.messaging.PaymentFailedEvent;
import com.backend.payment.messaging.PaymentSucceededEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class KafkaEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${payment.events.topic:payment.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishSucceeded(String orderId, String provider, String txnRef, long amount, String currency,
            Instant paidAt) {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "payment.succeeded",
                orderId,
                provider,
                txnRef,
                amount,
                currency,
                paidAt);
        kafkaTemplate.send(topic, orderId, event);
    }

    public void publishFailed(String orderId, String provider, String txnRef, long amount, String currency,
            String reasonCode) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                "payment.failed",
                orderId,
                provider,
                txnRef,
                amount,
                currency,
                reasonCode);
        kafkaTemplate.send(topic, orderId, event);
    }
}
