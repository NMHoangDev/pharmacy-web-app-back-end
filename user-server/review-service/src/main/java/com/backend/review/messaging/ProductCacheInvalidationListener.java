package com.backend.review.messaging;

import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import com.backend.review.service.ReviewService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ProductCacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidationListener.class);

    private final ReviewService reviewService;

    public ProductCacheInvalidationListener(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @KafkaListener(topics = TopicNames.CACHE_INVALIDATION_EVENTS, groupId = "review-service-cache")
    public void onMessage(@Payload EventEnvelope<?> event) {
        try {
            if (event == null || event.type() == null || event.payload() == null) {
                return;
            }

            String eventType = event.type();
            if (!EventTypes.PRODUCT_UPDATED.equalsIgnoreCase(eventType)
                    && !EventTypes.PRODUCT_CREATED.equalsIgnoreCase(eventType)
                    && !EventTypes.PRODUCT_DELETED.equalsIgnoreCase(eventType)) {
                return;
            }

            if (!(event.payload() instanceof Map<?, ?> payload)) {
                return;
            }

            Object sourceService = payload.get("service");
            if (sourceService != null && !"catalog".equalsIgnoreCase(sourceService.toString())) {
                return;
            }

            Object entityId = payload.get("entityId");
            if (entityId == null) {
                return;
            }

            reviewService.invalidateProductCacheByProductId(entityId.toString());
        } catch (Exception ex) {
            log.warn("Failed to process cache invalidation event: {}", ex.getMessage());
        }
    }
}
