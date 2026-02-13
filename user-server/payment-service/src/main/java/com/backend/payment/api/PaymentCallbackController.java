package com.backend.payment.api;

import com.backend.payment.messaging.PaymentStatusEvent;
import com.backend.payment.service.VNPayService;
import com.backend.payment.service.ZaloPayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentCallbackController {

    private final VNPayService vnpayService;
    private final ZaloPayService zalopayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentCallbackController(VNPayService vnpayService, ZaloPayService zalopayService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.vnpayService = vnpayService;
        this.zalopayService = zalopayService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        boolean isValid = vnpayService.verifyIpn(new HashMap<>(params));
        Map<String, String> response = new HashMap<>();

        if (!isValid) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return ResponseEntity.ok(response);
        }

        String txnRef = params.get("vnp_TxnRef");
        String orderId = txnRef.split("_")[0];
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String status = "00".equals(vnpResponseCode) ? "PAID" : "FAILED";

        publishPaymentStatus(orderId, status, "VNPAY", params.get("vnp_TransactionNo"));

        response.put("RspCode", "00");
        response.put("Message", "Confirm Success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/zalopay-callback")
    public ResponseEntity<Map<String, Object>> zalopayCallback(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean isValid = zalopayService.verifyCallback(params);
            if (!isValid) {
                result.put("return_code", -1);
                result.put("return_message", "mac not equal");
                return ResponseEntity.ok(result);
            }

            String dataStr = params.get("data").toString();
            Map dataMap = objectMapper.readValue(dataStr, Map.class);
            String embedDataStr = dataMap.get("embed_data").toString();
            Map embedDataMap = objectMapper.readValue(embedDataStr, Map.class);

            String orderId = embedDataMap.get("orderId").toString();
            publishPaymentStatus(orderId, "PAID", "ZALOPAY", dataMap.get("zp_trans_id").toString());

            result.put("return_code", 1);
            result.put("return_message", "success");
        } catch (Exception e) {
            result.put("return_code", 0);
            result.put("return_message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private void publishPaymentStatus(String orderId, String status, String provider, String transRef) {
        PaymentStatusEvent event = new PaymentStatusEvent(orderId, status, provider, transRef);
        kafkaTemplate.send("payment.status", orderId, event);
    }
}
