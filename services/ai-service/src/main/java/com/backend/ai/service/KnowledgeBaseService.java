package com.backend.ai.service;

import com.backend.ai.service.dto.KnowledgeHit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final ObjectMapper objectMapper;
    private final String pythonExecutable;
    private final String scriptPath;
    private final String dbPath;
    private final String dataDir;
    private final String collection;
    private final int topK;
    private final int timeoutSeconds;
    private final long cacheTtlMillis;
    private final long failureCooldownMillis;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private volatile long disabledUntil = 0L;

    public KnowledgeBaseService(
            ObjectMapper objectMapper,
            @Value("${ai.knowledge.python-executable:D:/pharmacy-app/AI/.venv/Scripts/python.exe}") String pythonExecutable,
            @Value("${ai.knowledge.script-path:D:/pharmacy-app/web-app/back-end/pharmacy-app/user-server/ai-service/scripts/query_kb.py}") String scriptPath,
            @Value("${ai.knowledge.db-path:D:/pharmacy-app/AI/db}") String dbPath,
            @Value("${ai.knowledge.data-dir:D:/pharmacy-app/AI/data/medical}") String dataDir,
            @Value("${ai.knowledge.collection:medical_kb}") String collection,
            @Value("${ai.knowledge.top-k:5}") int topK,
            @Value("${ai.knowledge.timeout-seconds:20}") int timeoutSeconds,
            @Value("${ai.knowledge.cache-ttl-seconds:600}") long cacheTtlSeconds,
            @Value("${ai.knowledge.failure-cooldown-seconds:30}") long failureCooldownSeconds) {
        this.objectMapper = objectMapper;
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.dbPath = dbPath;
        this.dataDir = dataDir;
        this.collection = collection;
        this.topK = topK;
        this.timeoutSeconds = timeoutSeconds;
        this.cacheTtlMillis = cacheTtlSeconds * 1000L;
        this.failureCooldownMillis = failureCooldownSeconds * 1000L;
    }

    public List<KnowledgeHit> search(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        if (System.currentTimeMillis() < disabledUntil) {
            log.info("knowledge search skipped during cooldown for question='{}'", question);
            return List.of();
        }
        String cacheKey = question.trim().toLowerCase();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMillis)) {
            log.info("knowledge search cache hit: question='{}', hitCount={}", question, cached.hits().size());
            return cached.hits();
        }
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                scriptPath,
                "--query",
                question,
                "--db-path",
                dbPath,
                "--data-dir",
                dataDir,
                "--collection",
                collection,
                "--top-k",
                String.valueOf(topK));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (left, right) -> left + right);
            }
            if (!finished) {
                process.destroyForcibly();
                log.warn("knowledge search timed out after {}s for question='{}'", timeoutSeconds, question);
                disabledUntil = System.currentTimeMillis() + failureCooldownMillis;
                return List.of();
            }
            if (process.exitValue() != 0) {
                log.warn("knowledge search exited with code {}: {}", process.exitValue(), output);
                disabledUntil = System.currentTimeMillis() + failureCooldownMillis;
                return List.of();
            }
            JsonNode root = objectMapper.readTree(output);
            List<KnowledgeHit> hits = new ArrayList<>();
            JsonNode items = root.path("hits");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    hits.add(new KnowledgeHit(
                            item.path("title").asText("Khong ro ten"),
                            item.path("genericName").asText(""),
                            item.path("snippet").asText(""),
                            item.path("source").asText(root.path("backend").asText("knowledge")),
                            item.path("type").asText("knowledge"),
                            item.path("topic").asText(""),
                            item.path("audience").asText(""),
                            item.path("riskLevel").asText(""),
                            item.path("warning").asText(""),
                            item.hasNonNull("score") ? item.path("score").asInt() : null));
                }
            }
            log.info("knowledge search backend={}, hitCount={}", root.path("backend").asText("unknown"), hits.size());
            disabledUntil = 0L;
            cache.put(cacheKey, new CacheEntry(hits, System.currentTimeMillis()));
            return hits;
        } catch (Exception ex) {
            log.warn("knowledge search failed: {}", ex.getMessage(), ex);
            disabledUntil = System.currentTimeMillis() + failureCooldownMillis;
            return List.of();
        }
    }

    private record CacheEntry(List<KnowledgeHit> hits, long createdAt) {
        private boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }
    }
}
