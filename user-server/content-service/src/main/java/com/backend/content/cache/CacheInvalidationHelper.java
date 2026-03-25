package com.backend.content.cache;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationHelper {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationHelper.class);

    private static final int SCAN_BATCH_SIZE = 500;

    private final RedisTemplate<String, ?> redisTemplate;

    public CacheInvalidationHelper(RedisTemplate<String, ?> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // =========================
    // DELETE SINGLE KEY
    // =========================
    public void deleteKey(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("[CACHE DELETE] service={} key={} deleted={}", CacheConstants.SERVICE_NAME, key,
                    Boolean.TRUE.equals(deleted));
        } catch (DataAccessException ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, key, ex.getMessage());
        }
    }

    // =========================
    // DELETE BY PATTERN (SCAN)
    // =========================
    public void deleteByPattern(String pattern) {
        try {
            Long deletedCount = redisTemplate.execute((RedisCallback<Long>) connection -> {
                return scanAndDelete(connection, pattern);
            });

            log.info("[CACHE DELETE] service={} key={} deleted={}", CacheConstants.SERVICE_NAME, pattern, deletedCount);

        } catch (DataAccessException ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, pattern,
                    ex.getMessage());
        }
    }

    // =========================
    // INVALIDATION LOGIC
    // =========================
    public void invalidateEntityCache(String prefix, Long id) {
        invalidateList(prefix);
        if (id != null) {
            invalidateDetail(prefix, String.valueOf(id));
        }
    }

    public void invalidateEntityCache(String prefix, String id) {
        invalidateList(prefix);
        if (id != null && !id.isBlank()) {
            invalidateDetail(prefix, id);
        }
    }

    private void invalidateList(String prefix) {
        deleteByPattern(prefix + ":list:*");
    }

    private void invalidateDetail(String prefix, String id) {
        deleteKey(prefix + ":detail:" + id);
        deleteByPattern(prefix + ":detail:" + id + ":*");
    }

    // =========================
    // CORE SCAN DELETE
    // =========================
    private Long scanAndDelete(RedisConnection connection, String pattern) {

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_BATCH_SIZE)
                .build();

        List<byte[]> buffer = new ArrayList<>(SCAN_BATCH_SIZE);
        long totalDeleted = 0;

        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {

            while (cursor.hasNext()) {
                buffer.add(cursor.next());

                if (buffer.size() >= SCAN_BATCH_SIZE) {
                    totalDeleted += deleteBatch(connection, buffer);
                    buffer.clear();
                }
            }
        } catch (Exception ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, pattern,
                    ex.getMessage());
            return totalDeleted;
        }

        if (!buffer.isEmpty()) {
            totalDeleted += deleteBatch(connection, buffer);
        }

        return totalDeleted;
    }

    private long deleteBatch(RedisConnection connection, List<byte[]> keys) {
        if (keys.isEmpty())
            return 0;

        // 🔥 Prefer UNLINK (non-blocking) if supported
        try {
            return connection.keyCommands().unlink(keys.toArray(new byte[0][]));
        } catch (Exception ex) {
            // fallback to DEL
            try {
                connection.keyCommands().del(keys.toArray(new byte[0][]));
                return keys.size();
            } catch (Exception delEx) {
                log.error("[CACHE ERROR] service={} key=bulk-delete message={}", CacheConstants.SERVICE_NAME,
                        delEx.getMessage());
                return 0;
            }
        }
    }
}