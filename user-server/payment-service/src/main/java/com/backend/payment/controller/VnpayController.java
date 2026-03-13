package com.backend.payment.controller;

import com.backend.payment.api.dto.VnpayCreateRequest;
import com.backend.payment.api.dto.VnpayCreateResponse;
import com.backend.payment.api.dto.VnpayIpnResponse;
import com.backend.payment.service.VNPayService;
import com.backend.payment.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
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
public class VnpayController {
    private final VNPayService vnpayService;

    public VnpayController(VNPayService vnpayService) {
        this.vnpayService = vnpayService;
    }

    @PostMapping("/vnpay/create")
    public ResponseEntity<VnpayCreateResponse> create(@Valid @RequestBody VnpayCreateRequest request,
            HttpServletRequest httpServletRequest) {
        String ipAddress = IpUtil.getClientIp(httpServletRequest);
        return ResponseEntity.ok(vnpayService.createPaymentUrl(request, ipAddress));
    }

    @GetMapping(value = "/vnpay/return", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleReturn(@RequestParam Map<String, String> params) {
        boolean valid = vnpayService.verifySignature(new HashMap<>(params));
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid checksum");
        }
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        String txnRef = params.getOrDefault("vnp_TxnRef", "");
        return ResponseEntity.ok("VNPAY return for txnRef=" + txnRef + ", success=" + success);
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> handleIpn(@RequestParam Map<String, String> params) {
        VnpayIpnResponse response = vnpayService.handleIpn(new HashMap<>(params));
        return ResponseEntity.ok(response);
    }
}
