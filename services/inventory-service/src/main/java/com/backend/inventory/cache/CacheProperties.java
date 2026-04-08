package com.backend.inventory.cache;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private long defaultTtl = 600L;
    private Map<String, Long> ttl = new HashMap<>();

    public long getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(long defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Long> getTtl() {
        return ttl;
    }

    public void setTtl(Map<String, Long> ttl) {
        this.ttl = ttl == null ? new HashMap<>() : ttl;
    }

    public long ttl(String key) {
        Long value = ttl.get(key);
        return value == null || value <= 0 ? defaultTtl : value;
    }
}
