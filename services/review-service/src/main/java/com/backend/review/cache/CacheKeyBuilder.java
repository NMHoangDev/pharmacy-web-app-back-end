package com.backend.review.cache;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CacheKeyBuilder {

    public String build(String entity, String type, Object... params) {
        String suffix = Arrays.stream(params)
                .filter(Objects::nonNull)
                .map(v -> String.valueOf(v).trim())
                .filter(v -> !v.isBlank())
                .map(v -> v.replace(":", "_"))
                .collect(Collectors.joining(":"));

        if (suffix.isBlank()) {
            return String.join(":", CacheConstants.SERVICE_NAME, entity, type);
        }
        return String.join(":", CacheConstants.SERVICE_NAME, entity, type, suffix);
    }

    public String list(String entity, Object... params) {
        return build(entity, "list", params);
    }

    public String detail(String entity, Object id) {
        return build(entity, "detail", id);
    }

    public String pattern(String entity, String type, Object... params) {
        return build(entity, type, params) + ":*";
    }
}
