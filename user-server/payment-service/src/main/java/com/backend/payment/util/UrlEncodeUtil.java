package com.backend.payment.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlEncodeUtil {
    private UrlEncodeUtil() {
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
