package com.pharmacy.discount.kafka;

import com.backend.common.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    public OrderEventListener() {
    }

    @KafkaListener(topics = "${kafka.topics.order-events:order.events}", groupId = "discount-service")
    public void onOrderEvent(@Payload EventEnvelope<?> event) {
        try {
            if (event == null || event.type() == null || event.payload() == null) {
                return;
            }

            // Current order-service emits ORDER_CREATED / ORDER_PAID / ORDER_STATUS_UPDATED
            String type = event.type();
            if (!"ORDER_PAID".equalsIgnoreCase(type) && !"ORDER_STATUS_UPDATED".equalsIgnoreCase(type)) {
                return;
            }

            if (!(event.payload() instanceof Map<?, ?> payload)) {
                return;
            }

            Object promoCode = payload.get("promoCode");
            if (promoCode == null) {
                // Order events currently do not include promoCode; keep listener future-ready.
                return;
            }

            Object orderId = payload.get("orderId");
            Object userId = payload.get("userId");
            log.info("Order completed event received orderId={} userId={} promoCode={}", orderId, userId, promoCode);

            // Future integration: update discount usages when order-service includes
            // promoCode/discountId.
        } catch (Exception ex) {
            log.warn("Failed to process order event: {}", ex.getMessage());
            throw ex;
        }
    }
}
