package com.backend.payment.service;

import com.backend.payment.api.dto.VnpayCreateRequest;
import com.backend.payment.api.dto.VnpayCreateResponse;
import com.backend.payment.config.VnpayProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class VNPayServiceTest {
    @Test
    void createPaymentUrl_generatesNumericTxnRef_andValidDates() {
        VnpayProperties props = new VnpayProperties();
        props.setTmnCode("QC6ST9D7");
        props.setHashSecret("secret");
        props.setUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        props.setReturnUrl("https://example.com/return");
        props.setIpnUrl("https://example.com/ipn");
        props.setVersion("2.1.0");
        props.setCommand("pay");
        props.setCurrCode("VND");
        props.setLocaleDefault("vn");
        props.setExpireMinutes(15);
        props.setTimezone("Etc/GMT+7");

        PaymentTransactionService txService = Mockito.mock(PaymentTransactionService.class);
        when(txService.existsByTxnRef(anyString())).thenReturn(false);
        doNothing().when(txService)
                .createPending(anyString(), Mockito.any(), anyString(), anyLong(), anyString());

        KafkaEventPublisher eventPublisher = Mockito.mock(KafkaEventPublisher.class);

        RestClient orderServiceRestClient = RestClient.builder().baseUrl("http://localhost:7019").build();

        VNPayService service = new VNPayService(props, txService, eventPublisher, orderServiceRestClient);
        VnpayCreateRequest request = new VnpayCreateRequest(
                "order-123",
                1000L,
                "Test order",
                null,
                null,
                null);

        VnpayCreateResponse response = service.createPaymentUrl(request, "bad-ip");

        assertTrue(response.txnRef().matches("\\d+"));
        assertTrue(response.txnRef().indexOf('_') < 0);
        assertTrue(response.txnRef().indexOf('-') < 0);

        Map<String, String> params = parseQuery(response.data());
        assertEquals("127.0.0.1", params.get("vnp_IpAddr"));
        assertTrue(params.get("vnp_CreateDate").matches("\\d{14}"));
        assertTrue(params.get("vnp_ExpireDate").matches("\\d{14}"));
    }

    private Map<String, String> parseQuery(String url) {
        int idx = url.indexOf('?');
        String query = idx >= 0 ? url.substring(idx + 1) : url;
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(decode(parts[0]), decode(parts[1]));
            }
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.US_ASCII);
    }
}
