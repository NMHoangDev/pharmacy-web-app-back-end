package com.backend.payment.api;

import com.backend.payment.api.dto.PaymentMethodResponse;
import com.backend.payment.api.dto.PaymentInitiateRequest;
import com.backend.payment.api.dto.PaymentInitiateResponse;
import com.backend.payment.service.VNPayService;
import com.backend.payment.service.ZaloPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            HttpServletRequest servletRequest) throws Exception {

        String paymentUrl = "";
        String provider = request.provider().toUpperCase();

        if ("VNPAY".equals(provider)) {
            String ip = servletRequest.getRemoteAddr();
            paymentUrl = vnpayService.createPaymentUrl(request, ip);
        } else if ("ZALOPAY".equals(provider)) {
            paymentUrl = zalopayService.createOrder(request);
        } else {
            return ResponseEntity.badRequest()
                    .body(new PaymentInitiateResponse(null, "Unsupported provider: " + provider));
        }

        return ResponseEntity.ok(new PaymentInitiateResponse(paymentUrl, "Success"));
    }
}
