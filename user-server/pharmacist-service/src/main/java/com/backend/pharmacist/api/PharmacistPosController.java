package com.backend.pharmacist.api;

import com.backend.pharmacist.api.dto.pos.CancelOfflineOrderRequest;
import com.backend.pharmacist.api.dto.pos.ConfirmOfflinePaymentRequest;
import com.backend.pharmacist.api.dto.pos.CreateOfflineOrderRequest;
import com.backend.pharmacist.api.dto.pos.OfflineOrderPageResponse;
import com.backend.pharmacist.api.dto.pos.OfflineOrderResponse;
import com.backend.pharmacist.api.dto.pos.OfflineReceiptResponse;
import com.backend.pharmacist.api.dto.pos.PosProductSearchPageResponse;
import com.backend.pharmacist.api.dto.pos.RefundOfflineOrderRequest;
import com.backend.pharmacist.model.OfflineOrderStatus;
import com.backend.pharmacist.service.PosOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacists/pos")
public class PharmacistPosController {

    private final PosOrderService posOrderService;

    public PharmacistPosController(PosOrderService posOrderService) {
        this.posOrderService = posOrderService;
    }

    @GetMapping("/products/search")
    public ResponseEntity<PosProductSearchPageResponse> searchProducts(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(posOrderService.searchProducts(query, branchId, page, size));
    }

    @PostMapping("/orders")
    public ResponseEntity<OfflineOrderResponse> createOrder(@RequestBody @Valid CreateOfflineOrderRequest request) {
        return ResponseEntity.ok(posOrderService.createOfflineOrder(request));
    }

    @GetMapping("/orders")
    public ResponseEntity<OfflineOrderPageResponse> listOrders(
            @RequestParam(name = "range", required = false) String range,
            @RequestParam(name = "status", required = false) OfflineOrderStatus status,
            @RequestParam(name = "pharmacistId", required = false) UUID pharmacistId,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(posOrderService.listOrders(range, status, pharmacistId, branchId, page, size));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OfflineOrderResponse> getOrder(@PathVariable("id") UUID orderId) {
        return ResponseEntity.ok(posOrderService.getOrder(orderId));
    }

    @PostMapping("/orders/{id}/pay")
    public ResponseEntity<OfflineOrderResponse> confirmPayment(
            @PathVariable("id") UUID orderId,
            @RequestBody @Valid ConfirmOfflinePaymentRequest request) {
        return ResponseEntity.ok(posOrderService.confirmPayment(orderId, request));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OfflineOrderResponse> cancelOrder(
            @PathVariable("id") UUID orderId,
            @RequestBody @Valid CancelOfflineOrderRequest request) {
        return ResponseEntity.ok(posOrderService.cancelOrder(orderId, request));
    }

    @PostMapping("/orders/{id}/refund")
    public ResponseEntity<OfflineOrderResponse> refundOrder(
            @PathVariable("id") UUID orderId,
            @RequestBody @Valid RefundOfflineOrderRequest request) {
        return ResponseEntity.ok(posOrderService.refundOrder(orderId, request));
    }

    @GetMapping("/orders/{id}/receipt")
    public ResponseEntity<OfflineReceiptResponse> printReceipt(@PathVariable("id") UUID orderId) {
        return ResponseEntity.ok(new OfflineReceiptResponse(posOrderService.printReceipt(orderId)));
    }
}
