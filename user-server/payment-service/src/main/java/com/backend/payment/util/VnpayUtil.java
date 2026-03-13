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
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isBlank()) {
                continue;
            }
            if (!first) {
                hashData.append('&');
            }
            first = false;
            hashData.append(UrlEncodeUtil.encode(fieldName)).append('=')
                    .append(UrlEncodeUtil.encode(fieldValue));
        }
        return hashData.toString();
    }

    public static String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isBlank()) {
                continue;
            }
            if (!first) {
                query.append('&');
            }
            first = false;
            query.append(UrlEncodeUtil.encode(fieldName)).append('=')
                    .append(UrlEncodeUtil.encode(fieldValue));
        }
        return query.toString();
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
