package com.backend.payment.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {
    private static final DateTimeFormatter VNPAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DateTimeUtil() {
    }

    public static String nowVnpay(ZoneId zoneId) {
        return LocalDateTime.now(zoneId).format(VNPAY_FORMAT);
    }

    public static String plusMinutesVnpay(ZoneId zoneId, int minutes) {
        return LocalDateTime.now(zoneId).plusMinutes(minutes).format(VNPAY_FORMAT);
    }

    public static String todayVnpayDate(ZoneId zoneId) {
        return LocalDateTime.now(zoneId).format(VNPAY_DATE);
    }

    public static Instant parseVnpayPayDate(String value, ZoneId zoneId) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.parse(value, VNPAY_FORMAT);
        return localDateTime.atZone(zoneId).toInstant();
    }
}
