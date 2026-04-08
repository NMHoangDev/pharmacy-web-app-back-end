package com.backend.user.cache;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

    private final RedisTemplate<String, Object> redisObjectTemplate;

    public CacheInvalidationHelper(RedisTemplate<String, Object> redisObjectTemplate) {
        this.redisObjectTemplate = redisObjectTemplate;
    }

    public void deleteKey(String key) {
        try {
            Boolean deleted = redisObjectTemplate.delete(key);
            log.info("[CACHE DELETE] service={} key={} deleted={}", CacheConstants.SERVICE_NAME, key, Boolean.TRUE.equals(deleted));
        } catch (DataAccessException ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, key, ex.getMessage());
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            redisObjectTemplate.execute((RedisCallback<Void>) connection -> {
                scanAndDelete(connection, pattern);
                return null;
            });
            log.info("[CACHE DELETE] service={} key={}", CacheConstants.SERVICE_NAME, pattern);
        } catch (DataAccessException ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, pattern, ex.getMessage());
        }
    }

    public void invalidateEntityCache(String prefix, Long id) {
        deleteByPattern(prefix + ":list:*");
        if (id != null) {
            deleteKey(prefix + ":detail:" + id);
            deleteByPattern(prefix + ":detail:" + id + ":*");
        }
    }

    public void invalidateEntityCache(String prefix, String id) {
        deleteByPattern(prefix + ":list:*");
        if (id != null && !id.isBlank()) {
            deleteKey(prefix + ":detail:" + id);
            deleteByPattern(prefix + ":detail:" + id + ":*");
        }
    }

    private void scanAndDelete(RedisConnection connection, String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();
        List<byte[]> buffer = new ArrayList<>(SCAN_BATCH_SIZE);

        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext()) {
                buffer.add(cursor.next());
                if (buffer.size() >= SCAN_BATCH_SIZE) {
                    connection.keyCommands().del(buffer.toArray(new byte[0][]));
                    buffer.clear();
                }
            }
        } catch (Exception ex) {
            log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, pattern, ex.getMessage());
            return;
        }

        if (!buffer.isEmpty()) {
            try {
                connection.keyCommands().del(buffer.toArray(new byte[0][]));
            } catch (Exception ex) {
                log.error("[CACHE ERROR] service={} key={} message={}", CacheConstants.SERVICE_NAME, pattern, ex.getMessage());
            }
        }
    }
}
