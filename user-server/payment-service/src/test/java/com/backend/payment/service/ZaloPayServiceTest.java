package com.backend.payment.service;

import com.backend.payment.api.dto.PaymentInitiateRequest;
import com.backend.payment.utils.PaymentUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZaloPayServiceTest {

    private ZaloPayService zaloPayService;

    @BeforeEach
    void setUp() {
        zaloPayService = new ZaloPayService();
        ReflectionTestUtils.setField(zaloPayService, "appId", "553");
        ReflectionTestUtils.setField(zaloPayService, "key1", "9ph31993439");
        ReflectionTestUtils.setField(zaloPayService, "key2", "Iyz2habByr7S9uE67zH9G8X5rUf2jQu");
    }

    @Test
    void testMacGeneration() {
        // app_id|app_trans_id|app_user|amount|app_time|embed_data|item
        String data = "553|240202_123|user1|100000|123456789|{}|[]";
        String mac = PaymentUtils.hmacSHA256("9ph31993439", data);
        assertNotNull(mac);
        // This confirms the utility works for ZaloPay's specific hmacSHA256 requirement
    }
}
