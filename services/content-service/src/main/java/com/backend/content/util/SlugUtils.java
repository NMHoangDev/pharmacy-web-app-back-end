package com.backend.content.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
        if (slug.length() > 160) {
            slug = slug.substring(0, 160).replaceAll("-+$", "");
        }
        return slug;
    }

    public static String uniqueSlug(String base, Function<String, Boolean> exists) {
        String candidate = base;
        int idx = 2;
        while (exists.apply(candidate)) {
            candidate = base + "-" + idx;
            idx++;
        }
        return candidate;
    }
}
