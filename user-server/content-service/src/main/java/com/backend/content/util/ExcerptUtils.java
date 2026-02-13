package com.backend.content.util;

public final class ExcerptUtils {

    private ExcerptUtils() {
    }

    public static String toExcerpt(String input, int maxLen) {
        if (input == null) {
            return "";
        }
        String plain = input.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= maxLen) {
            return plain;
        }
        return plain.substring(0, maxLen).trim();
    }
}
