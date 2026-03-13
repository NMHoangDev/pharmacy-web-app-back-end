package com.backend.payment.service;

import com.backend.payment.api.dto.ZaloPayCreateRequest;
import com.backend.payment.api.dto.ZaloPayCreateResponse;
import com.backend.payment.config.ZaloPayProperties;
import com.backend.payment.model.PaymentProvider;
import com.backend.payment.model.PaymentStatus;
import com.backend.payment.model.PaymentTransaction;
import com.backend.payment.util.HmacUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ZaloPayService {
    private static final DateTimeFormatter APP_TRANS_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final ZaloPayProperties properties;
    private final PaymentTransactionService transactionService;
    private final KafkaEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ZaloPayService(ZaloPayProperties properties,
            PaymentTransactionService transactionService,
            KafkaEventPublisher eventPublisher) {
        this.properties = properties;
        this.transactionService = transactionService;
        this.eventPublisher = eventPublisher;
    }

    public ZaloPayCreateResponse createOrder(ZaloPayCreateRequest request) {
        String appTransId = generateAppTransId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("app_id", properties.getAppId());
        payload.put("app_trans_id", appTransId);
        payload.put("app_time", System.currentTimeMillis());
        payload.put("app_user", "user_" + request.orderId());
        payload.put("amount", request.amount());
        payload.put("description", request.description());
        payload.put("bank_code", "");

        List<Map<String, Object>> items = request.items() == null ? List.of() : request.items();
        String itemJson = toJson(items);
        payload.put("item", itemJson);

        Map<String, Object> embed = request.embedData() == null ? new HashMap<>() : new HashMap<>(request.embedData());
        embed.putIfAbsent("orderId", request.orderId());
        embed.putIfAbsent("redirectUrl", properties.getRedirectUrl());
        String embedJson = toJson(embed);
        payload.put("embed_data", embedJson);
        payload.put("callback_url", properties.getCallbackUrl());

        String data = payload.get("app_id") + "|" + payload.get("app_trans_id") + "|"
                + payload.get("app_user") + "|" + payload.get("amount") + "|"
                + payload.get("app_time") + "|" + payload.get("embed_data") + "|" + payload.get("item");
        payload.put("mac", HmacUtil.hmacSha256(properties.getKey1(), data));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(properties.getEndpoint(), entity, Map.class);
        Map body = response.getBody();
        if (body == null || !"1".equals(String.valueOf(body.get("return_code")))) {
            String message = body == null ? "Unknown error" : String.valueOf(body.get("return_message"));
            throw new IllegalStateException("ZaloPay Error: " + message);
        }

        transactionService.createPending(request.orderId(), PaymentProvider.ZALOPAY, appTransId, request.amount(),
                "VND");
        return new ZaloPayCreateResponse(String.valueOf(body.get("order_url")), appTransId);
    }

    public boolean verifyCallback(Map<String, Object> params) {
        try {
            String dataStr = String.valueOf(params.get("data"));
            String reqMac = String.valueOf(params.get("mac"));
            String mac = HmacUtil.hmacSha256(properties.getKey2(), dataStr);
            return mac.equalsIgnoreCase(reqMac);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean handleCallback(Map<String, Object> params) {
        if (!verifyCallback(params)) {
            return false;
        }

        Map<String, Object> dataMap = parseJsonMap(String.valueOf(params.get("data")));
        String txnRef = String.valueOf(dataMap.get("app_trans_id"));
        Optional<PaymentTransaction> txOptional = transactionService.findByTxnRef(txnRef);
        if (txOptional.isEmpty()) {
            return true;
        }

        PaymentTransaction tx = txOptional.get();
        String zpTransId = dataMap.get("zp_trans_id") == null ? null : String.valueOf(dataMap.get("zp_trans_id"));
        String returnCode = dataMap.get("return_code") == null ? null : String.valueOf(dataMap.get("return_code"));
        String status = dataMap.get("status") == null ? null : String.valueOf(dataMap.get("status"));
        boolean success = "1".equals(returnCode) || "1".equals(status);

        PaymentStatus newStatus = success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        Instant paidAt = success ? Instant.now() : null;
        String rawCallback = toJson(params);

        Optional<PaymentTransaction> updated = transactionService.updateIfPending(txnRef, newStatus, returnCode,
                status, zpTransId, null, rawCallback, paidAt);
        if (updated.isEmpty()) {
            return true;
        }

        if (success) {
            eventPublisher.publishSucceeded(tx.getOrderId(), PaymentProvider.ZALOPAY.name(), txnRef, tx.getAmount(),
                    tx.getCurrency(), paidAt == null ? Instant.now() : paidAt);
        } else {
            eventPublisher.publishFailed(tx.getOrderId(), PaymentProvider.ZALOPAY.name(), txnRef, tx.getAmount(),
                    tx.getCurrency(), returnCode == null ? "FAILED" : returnCode);
        }
        return true;
    }

    private String generateAppTransId() {
        String datePrefix = LocalDateTime.now(ZoneId.systemDefault()).format(APP_TRANS_DATE);
        String candidate = datePrefix + "_" + (int) (Math.random() * 1_000_000);
        if (!transactionService.existsByTxnRef(candidate)) {
            return candidate;
        }
        return datePrefix + "_" + System.nanoTime();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
