package com.backend.payment.api;

import com.backend.payment.api.dto.PaymentInitiateRequest;
import com.backend.payment.api.dto.PaymentInitiateResponse;
import com.backend.payment.api.dto.PaymentMethodResponse;
import com.backend.payment.api.dto.VnpayCreateRequest;
import com.backend.payment.api.dto.ZaloPayCreateRequest;
import com.backend.payment.api.dto.ZaloPayCreateResponse;
import com.backend.payment.api.dto.VnpayCreateResponse;
import com.backend.payment.service.VNPayService;
import com.backend.payment.service.ZaloPayService;
import com.backend.payment.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final VNPayService vnpayService;
    private final ZaloPayService zalopayService;

    public PaymentController(VNPayService vnpayService, ZaloPayService zalopayService) {
        this.vnpayService = vnpayService;
        this.zalopayService = zalopayService;
    }

    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodResponse>> getMethods() {
        return ResponseEntity.ok(List.of(
                new PaymentMethodResponse("COD", "Thanh toán khi nhận hàng (COD)", "attach_money"),
                new PaymentMethodResponse("BANK_TRANSFER", "Chuyển khoản ngân hàng", "account_balance"),
                new PaymentMethodResponse("VNPAY", "Ví điện tử VNPAY", "payments"),
                new PaymentMethodResponse("ZALOPAY", "Ví điện tử ZaloPay", "qr_code_scanner")));
    }

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiate(
            @Valid @RequestBody PaymentInitiateRequest request,
            HttpServletRequest servletRequest) {
        String paymentUrl;
        String provider = request.provider().toUpperCase();

        if ("VNPAY".equals(provider)) {
            String ip = IpUtil.getClientIp(servletRequest);
            VnpayCreateRequest vnpayRequest = new VnpayCreateRequest(
                    request.orderId(),
                    (long) request.amount(),
                    request.orderInfo() == null ? "Thanh toan don hang: " + request.orderId() : request.orderInfo(),
                    "other",
                    null,
                    null);
            VnpayCreateResponse response = vnpayService.createPaymentUrl(vnpayRequest, ip);
            paymentUrl = response.data();
        } else if ("ZALOPAY".equals(provider)) {
            ZaloPayCreateRequest zaloPayRequest = new ZaloPayCreateRequest(
                    request.orderId(),
                    (long) request.amount(),
                    request.orderInfo() == null ? "Thanh toan don hang: " + request.orderId() : request.orderInfo(),
                    List.of(),
                    Map.of("orderId", request.orderId()));
            ZaloPayCreateResponse response = zalopayService.createOrder(zaloPayRequest);
            paymentUrl = response.paymentUrl();
        } else {
            return ResponseEntity.badRequest()
                    .body(new PaymentInitiateResponse(null, "Unsupported provider: " + provider));
        }

        return ResponseEntity.ok(new PaymentInitiateResponse(paymentUrl, "Success"));
    }
}
