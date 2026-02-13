package com.backend.payment.service;

import com.backend.payment.api.dto.PaymentInitiateRequest;
import com.backend.payment.utils.PaymentUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ZaloPayService {

    @Value("${payment.zalopay.app-id:553}")
    private String appId;

    @Value("${payment.zalopay.key1:9ph31993439}")
    private String key1;

    @Value("${payment.zalopay.key2:9ph31993439}")
    private String key2;

    @Value("${payment.zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}")
    private String endpoint;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createOrder(PaymentInitiateRequest request) throws Exception {
        String app_trans_id = new java.text.SimpleDateFormat("yyMMdd").format(new Date()) + "_"
                + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> order = new HashMap<>();
        order.put("app_id", appId);
        order.put("app_trans_id", app_trans_id);
        order.put("app_time", System.currentTimeMillis());
        order.put("app_user", "user_" + request.orderId());
        order.put("amount", (long) request.amount());
        order.put("description",
                request.orderInfo() != null ? request.orderInfo() : "Payment for order " + request.orderId());
        order.put("bank_code", "");
        order.put("item", "[]");

        Map<String, String> embedData = new HashMap<>();
        embedData.put("orderId", request.orderId());
        String embedDataStr = objectMapper.writeValueAsString(embedData);
        order.put("embed_data", embedDataStr);

        String data = order.get("app_id") + "|" + order.get("app_trans_id") + "|" + order.get("app_user") + "|"
                + order.get("amount")
                + "|" + order.get("app_time") + "|" + order.get("embed_data") + "|" + order.get("item");
        order.put("mac", PaymentUtils.hmacSHA256(key1, data));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(order, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
        Map body = response.getBody();

        if (body != null && body.get("return_code").toString().equals("1")) {
            return body.get("order_url").toString();
        } else {
            throw new RuntimeException(
                    "ZaloPay Error: " + (body != null ? body.get("return_message") : "Unknown error"));
        }
    }

    public boolean verifyCallback(Map<String, Object> params) {
        try {
            String dataStr = params.get("data").toString();
            String reqMac = params.get("mac").toString();
            String mac = PaymentUtils.hmacSHA256(key2, dataStr);
            return mac.equalsIgnoreCase(reqMac);
        } catch (Exception e) {
            return false;
        }
    }
}
