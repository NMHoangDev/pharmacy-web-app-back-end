package com.backend.content.cache;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);
    private static final long DEFAULT_TTL_SECONDS = 600L;
    private static final long MAX_TTL_JITTER_SECONDS = 30L;
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_BREAKER_OPEN_MILLIS = 30_000L;

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private final CacheInvalidationHelper cacheInvalidationHelper;
    private final CacheProperties cacheProperties;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong bypassCacheUntilEpochMillis = new AtomicLong(0L);

    public CacheHelper(RedisTemplate<String, Object> redisObjectTemplate,
            CacheInvalidationHelper cacheInvalidationHelper,
            CacheProperties cacheProperties) {
        this.redisObjectTemplate = redisObjectTemplate;
        this.cacheInvalidationHelper = cacheInvalidationHelper;
        this.cacheProperties = cacheProperties;
    }

    public <T> T getOrSetCacheByTtlKey(String key, String ttlKey, Supplier<T> dbSupplier) {
        return getOrSetCache(key, cacheProperties.ttl(ttlKey), dbSupplier);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrSetCache(String key, long ttlSeconds, Supplier<T> dbSupplier) {
        long safeTtl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;

        if (isCacheBypassed()) {
            log.warn("[CACHE FALLBACK] service={} key={}", CacheConstants.SERVICE_NAME, key);
            return dbSupplier.get();
        }

        try {
            Object cached = redisObjectTemplate.opsForValue().get(key);
            if (cached != null) {
                resetFailureState();
                log.info("[CACHE HIT] service={} key={}", CacheConstants.SERVICE_NAME, key);
                return (T) cached;
            }

            resetFailureState();
            log.info("[CACHE MISS] service={} key={}", CacheConstants.SERVICE_NAME, key);
        } catch (DataAccessException ex) {
            onRedisFailure(key, ex);
            log.warn("[CACHE FALLBACK] service={} key={}", CacheConstants.SERVICE_NAME, key);
            return dbSupplier.get();
        }

        T value = dbSupplier.get();
        if (value == null) {
            return null;
        }

        Duration ttl = Duration.ofSeconds(withJitter(safeTtl));
        try {
            redisObjectTemplate.opsForValue().set(key, value, ttl);
            resetFailureState();
            log.info("[CACHE SET] service={} key={} ttlSeconds={}", CacheConstants.SERVICE_NAME, key, ttl.getSeconds());
        } catch (DataAccessException ex) {
            onRedisFailure(key, ex);
        }
        return value;
    }

    public <T> T getOrSetCache(String key, Duration ttl, Supplier<T> dbSupplier) {
        long ttlSeconds = ttl == null ? DEFAULT_TTL_SECONDS : ttl.getSeconds();
        return getOrSetCache(key, ttlSeconds, dbSupplier);
    }

    public void evict(String key) {
        cacheInvalidationHelper.deleteKey(key);
    }

    public void evictByPattern(String pattern) {
        cacheInvalidationHelper.deleteByPattern(pattern);
    }

    private boolean isCacheBypassed() {
        return System.currentTimeMillis() < bypassCacheUntilEpochMillis.get();
    }

    private long withJitter(long ttlSeconds) {
        long jitter = ThreadLocalRandom.current().nextLong(MAX_TTL_JITTER_SECONDS + 1);
        return ttlSeconds + jitter;
    }

    private void onRedisFailure(String key, Exception ex) {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            bypassCacheUntilEpochMillis.set(System.currentTimeMillis() + CIRCUIT_BREAKER_OPEN_MILLIS);
        }
        log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, key, ex.getMessage());
    }

    private void resetFailureState() {
        consecutiveFailures.set(0);
        bypassCacheUntilEpochMillis.set(0L);
    }
}
