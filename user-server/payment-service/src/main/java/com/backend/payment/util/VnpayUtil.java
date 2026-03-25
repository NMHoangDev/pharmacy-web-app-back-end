package com.backend.payment.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class VnpayUtil {
    private VnpayUtil() {
    }

    public static String buildHashData(Map<String, String> params) {
        // VNPay official sample: hashData uses sorted keys ASC, skips null/empty,
        // and URL-encodes VALUE only (KEY is not encoded).
        return buildHashParamString(params);
    }

    public static String buildQueryString(Map<String, String> params) {
        // VNPay official sample: query string URL-encodes both KEY and VALUE.
        return buildQueryParamString(params);
    }

    private static String buildHashParamString(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean[] first = new boolean[] { true };

        forEachSortedNonBlank(params, (fieldName, trimmedValue) -> {
            if (!first[0]) {
                builder.append('&');
            }
            first[0] = false;

            builder.append(fieldName);
            builder.append('=');
            builder.append(UrlEncodeUtil.encode(trimmedValue));
        });

        return builder.toString();
    }

    private static String buildQueryParamString(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        boolean[] first = new boolean[] { true };

        forEachSortedNonBlank(params, (fieldName, trimmedValue) -> {
            if (!first[0]) {
                builder.append('&');
            }
            first[0] = false;

            builder.append(UrlEncodeUtil.encode(fieldName));
            builder.append('=');
            builder.append(UrlEncodeUtil.encode(trimmedValue));
        });

        return builder.toString();
    }

    private interface ParamConsumer {
        void accept(String fieldName, String trimmedValue);
    }

    private static void forEachSortedNonBlank(Map<String, String> params, ParamConsumer consumer) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null) {
                continue;
            }

            String trimmedValue = fieldValue.trim();
            if (trimmedValue.isEmpty()) {
                continue;
            }

            consumer.accept(fieldName, trimmedValue);
        }
    }

    public static String sanitizeOrderInfo(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String cleaned = normalized.replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 255) {
            return cleaned.substring(0, 255);
        }
        return cleaned;
    }
}
