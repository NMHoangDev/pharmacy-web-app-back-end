package com.backend.payment.api;

import com.backend.payment.api.dto.VnpayIpnResponse;
import com.backend.payment.service.VNPayService;
import com.backend.payment.service.ZaloPayService;
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
@RequestMapping("/api/payments")
public class PaymentCallbackController {

    private final VNPayService vnpayService;
    private final ZaloPayService zalopayService;

    public PaymentCallbackController(VNPayService vnpayService, ZaloPayService zalopayService) {
        this.vnpayService = vnpayService;
        this.zalopayService = zalopayService;
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<VnpayIpnResponse> vnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(vnpayService.handleIpn(new HashMap<>(params)));
    }

    @PostMapping("/zalopay-callback")
    public ResponseEntity<Map<String, Object>> zalopayCallback(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean ok = zalopayService.handleCallback(params);
            if (!ok) {
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
                return ResponseEntity.ok(result);
            }
            result.put("return_code", 1);
            result.put("return_message", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("return_code", 0);
            result.put("return_message", "error");
            return ResponseEntity.ok(result);
        }
    }
}
