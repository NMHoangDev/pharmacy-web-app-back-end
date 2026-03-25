package com.backend.payment.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class UrlEncodeUtil {
    private UrlEncodeUtil() {
    }

    public static String encode(String value) {
        // VNPay official sample uses URLEncoder with US_ASCII.
        // Note: URLEncoder encodes spaces as '+'. Do NOT replace '+' with '%20' for
        // VNPay signature.
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
