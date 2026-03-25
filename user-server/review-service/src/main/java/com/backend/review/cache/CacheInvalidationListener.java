package com.backend.review.cache;

import com.backend.common.model.EventEnvelope;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final CacheInvalidationHelper cacheInvalidationHelper;

    public CacheInvalidationListener(CacheInvalidationHelper cacheInvalidationHelper) {
        this.cacheInvalidationHelper = cacheInvalidationHelper;
    }

    @KafkaListener(topics = "${cache.invalidation.topic:cache-invalidation-events}", groupId = "review-cache-invalidation")
    public void onMessage(@Payload EventEnvelope<?> event) {
        try {
            if (event == null || !(event.payload() instanceof Map<?, ?> payload)) {
                return;
            }
            Object entity = payload.get("entity");
            Object entityId = payload.get("id");
            if (entity == null || entityId == null) {
                return;
            }
            cacheInvalidationHelper.invalidateEntityCache(String.valueOf(entity), String.valueOf(entityId));
        } catch (Exception ex) {
            log.warn("[CACHE ERROR] service={} key=listener message={}", CacheConstants.SERVICE_NAME, ex.getMessage());
        }
    }
}
