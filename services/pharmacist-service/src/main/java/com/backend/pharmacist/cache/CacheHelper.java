package com.backend.pharmacist.cache;

import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final CacheInvalidationHelper cacheInvalidationHelper;

    public CacheHelper(RedisTemplate<String, Object> redisObjectTemplate,
            CacheInvalidationHelper cacheInvalidationHelper) {
        this.redisObjectTemplate = redisObjectTemplate;
        this.cacheInvalidationHelper = cacheInvalidationHelper;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrSetCache(String key, long ttlSeconds, Supplier<T> dbSupplier) {
        long safeTtl = ttlSeconds > 0 ? ttlSeconds : 600L;
        Duration ttl = Duration.ofSeconds(safeTtl);
        try {
            Object cached = redisObjectTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache HIT: {}", key);
                return (T) cached;
            }

            log.debug("Cache MISS: {}", key);
            T value = dbSupplier.get();
            if (value != null) {
                redisObjectTemplate.opsForValue().set(key, value, ttl);
            }
            return value;
        } catch (Exception ex) {
            log.warn("Redis unavailable for key '{}'. Falling back to source. Reason: {}", key,
                    ex.getMessage());
            return dbSupplier.get();
        }
    }

    public <T> T getOrSetCache(String key, Duration ttl, Supplier<T> dbSupplier) {
        long ttlSeconds = ttl == null ? 600L : ttl.getSeconds();
        return getOrSetCache(key, ttlSeconds, dbSupplier);
    }

    public void evict(String key) {
        cacheInvalidationHelper.deleteKey(key);
    }

    public void evictByPattern(String pattern) {
        cacheInvalidationHelper.deleteByPattern(pattern);
    }
}
