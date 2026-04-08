package com.backend.payment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnpayUtilTest {
    @Test
    void sanitizeOrderInfo_removesDiacriticsAndSpecialChars() {
        String input = "Đơn hàng #123 - Áo?";
        String sanitized = VnpayUtil.sanitizeOrderInfo(input);

        assertTrue(sanitized.matches("[A-Za-z0-9 ]*"));
        assertEquals("Don hang 123 Ao", sanitized);
    }

    @Test
    void sanitizeOrderInfo_trimsAndLimitsLength() {
        String longInput = "a".repeat(300);
        String sanitized = VnpayUtil.sanitizeOrderInfo(longInput);

        assertEquals(255, sanitized.length());
    }
}
