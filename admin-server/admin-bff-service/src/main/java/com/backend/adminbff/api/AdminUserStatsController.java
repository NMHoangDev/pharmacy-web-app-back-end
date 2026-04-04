package com.backend.adminbff.api;

import com.backend.adminbff.dto.stats.AdminStatsEnvelope;
import com.backend.adminbff.dto.stats.UsersStatsResponse;
import com.backend.adminbff.service.AdminUserStatsService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserStatsController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserStatsController.class);
    private static final String CACHE_NAME = "admin-users-stats";

    private final AdminUserStatsService service;
    private final CacheManager cacheManager;

    public AdminUserStatsController(AdminUserStatsService service, CacheManager cacheManager) {
        this.service = service;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsEnvelope<UsersStatsResponse>> stats(
            @RequestParam(defaultValue = "7d") String range) {
        boolean cacheHitBeforeCall = false;
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            cacheHitBeforeCall = cache != null && cache.get(range) != null;
        } catch (RuntimeException ex) {
            log.warn("[ADMIN USERS STATS] cache probe failed for range={}. Continuing without cache probe.", range, ex);
        }

        UsersStatsResponse metrics = service.getStats(range);

        String cacheStatus = cacheHitBeforeCall ? "HIT" : "MISS";
        log.info("[ADMIN USERS STATS] range={} cache={}", range, cacheStatus);

        return ResponseEntity.ok()
                .header("X-Cache", cacheStatus)
                .body(new AdminStatsEnvelope<>(metrics, Instant.now(), range));
    }
}
