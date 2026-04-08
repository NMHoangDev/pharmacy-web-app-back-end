package com.backend.payment.controller;

import com.backend.payment.api.dto.ZaloPayCreateRequest;
import com.backend.payment.api.dto.ZaloPayCreateResponse;
import com.backend.payment.config.ZaloPayProperties;
import com.backend.payment.service.ZaloPayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({ "/payments", "/api/payments" })
public class ZaloPayController {
    private final ZaloPayService zaloPayService;
    private final ZaloPayProperties properties;

    public ZaloPayController(ZaloPayService zaloPayService, ZaloPayProperties properties) {
        this.zaloPayService = zaloPayService;
        this.properties = properties;
    }

    @PostMapping("/zalopay/create")
    public ResponseEntity<ZaloPayCreateResponse> create(@Valid @RequestBody ZaloPayCreateRequest request) {
        return ResponseEntity.ok(zaloPayService.createOrder(request));
    }

    @PostMapping("/zalopay/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean ok = zaloPayService.handleCallback(params);
            if (!ok) {
                response.put("return_code", -1);
                response.put("return_message", "mac not equal");
                return ResponseEntity.ok(response);
            }
            response.put("return_code", 1);
            response.put("return_message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("return_code", 0);
            response.put("return_message", "error");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/zalopay/return")
    public ResponseEntity<Void> handleReturn(@RequestParam(required = false) String app_trans_id,
            @RequestParam(required = false) String status) {
        String redirectUrl = properties.getRedirectUrl();
        StringBuilder target = new StringBuilder(redirectUrl);
        if (redirectUrl.contains("?")) {
            target.append("&");
        } else {
            target.append("?");
        }
        target.append("provider=zalopay");
        if (app_trans_id != null) {
            target.append("&txnRef=").append(app_trans_id);
        }
        if (status != null) {
            target.append("&success=").append("1".equals(status) || "true".equalsIgnoreCase(status));
        }
        return ResponseEntity.status(302).header("Location", target.toString()).build();
    }
}
