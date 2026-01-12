package com.backend.notification.messaging;

import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final NotificationSender sender;

    public OrderEventListener(NotificationSender sender) {
        this.sender = sender;
    }

    @KafkaListener(topics = TopicNames.ORDER_EVENTS, groupId = "notification-service")
    public void onMessage(@Payload EventEnvelope<String> event, Acknowledgment ack) {
        try {
            log.info("Received event type={} id={} payload={}", event.type(), event.id(), event.payload());
            if (EventTypes.ORDER_PAID.equals(event.type())) {
                sender.sendOrderPaid(event);
            } else if ("OrderCreated".equalsIgnoreCase(event.type())
                    || EventTypes.ORDER_CANCELLED.equals(event.type())) {
                sender.sendOrderCreated(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed processing event {}", event.id(), e);
            // let Kafka retry based on consumer config
        }
    }
}
