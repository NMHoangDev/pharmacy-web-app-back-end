package com.backend.order.service;

import com.backend.order.messaging.OrderEventTypes;
import com.backend.order.api.dto.*;
import com.backend.order.model.*;
import com.backend.order.repo.OrderRepository;
import com.backend.order.repo.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;

    public CheckoutService(OrderRepository orderRepository, OutboxEventRepository outboxRepository) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }

    public List<ShippingMethodResponse> getShippingMethods() {
        return List.of(
                new ShippingMethodResponse("STANDARD", "Giao hàng tiêu chuẩn", "Dự kiến nhận hàng: 1-3 ngày", 15000.0,
                        "15.000đ"),
                new ShippingMethodResponse("EXPRESS_2H", "Giao hàng hỏa tốc (2h)", "Nhận hàng trong 2 giờ", 45000.0,
                        "45.000đ"));
    }

    public CheckoutQuoteResponse computeQuote(CheckoutRequest request) {
        double subtotal = request.items().stream()
                .mapToDouble(i -> i.unitPrice() * i.quantity())
                .sum();

        double shippingFee = calculateShippingFee(request.shippingMethod());
        double discountAmount = calculateDiscount(request.promoCode(), subtotal);
        double grandTotal = subtotal + shippingFee - discountAmount;

        return new CheckoutQuoteResponse(subtotal, shippingFee, discountAmount, grandTotal);
    }

    @Transactional
    public CheckoutResponse placeOrder(CheckoutRequest request) {
        CheckoutQuoteResponse quote = computeQuote(request);
        UUID orderId = UUID.randomUUID();

        OrderEntity order = new OrderEntity();
        order.setId(orderId);
        order.setUserId(request.userId());
        order.setOrderCode(generateOrderCode());
        order.setBranchId(request.branchId());
        order.setSubtotal(quote.subtotal());
        order.setShippingFee(quote.shippingFee());
        order.setDiscountAmount(quote.discountAmount());
        order.setTotalAmount(quote.grandTotal());
        order.setPaymentMethod(request.paymentMethod());
        order.setNote(request.note());

        // 1. Snapshot Shipping Address
        if (request.shippingAddress() != null) {
            OrderShippingAddress address = new OrderShippingAddress();
            address.setId(UUID.randomUUID());
            address.setFullName(request.shippingAddress().fullName());
            address.setPhone(request.shippingAddress().phone());
            address.setAddressLine(request.shippingAddress().addressLine());
            address.setProvinceCode(request.shippingAddress().provinceCode());
            address.setProvinceName(request.shippingAddress().provinceName());
            address.setDistrictCode(request.shippingAddress().districtCode());
            address.setDistrictName(request.shippingAddress().districtName());
            address.setWardCode(request.shippingAddress().wardCode());
            address.setWardName(request.shippingAddress().wardName());
            order.setShippingAddress(address);
        }

        // 2. Snapshot Shipping Method
        OrderShipping shipping = new OrderShipping();
        shipping.setId(UUID.randomUUID());
        shipping.setMethod(request.shippingMethod());
        shipping.setFee(quote.shippingFee());
        shipping.setEtaRange(request.shippingMethod().equals("EXPRESS_2H") ? "2 hours" : "1-3 days");
        order.setShipping(shipping);

        // 3. Snapshot Discount
        if (request.promoCode() != null && !request.promoCode().isBlank()) {
            OrderDiscount discount = new OrderDiscount();
            discount.setId(UUID.randomUUID());
            discount.setPromoCode(request.promoCode());
            discount.setDiscountAmount(quote.discountAmount());
            discount.setDescription("Applied promo code: " + request.promoCode());
            order.setDiscount(discount);
        }

        // 4. Snapshot Items
        for (var itemReq : request.items()) {
            OrderItem item = new OrderItem();
            item.setId(new OrderItemId(orderId, itemReq.productId()));
            item.setProductName(itemReq.productName());
            item.setUnitPrice(itemReq.unitPrice());
            item.setQuantity(itemReq.quantity());
            order.addItem(item);
        }

        // 5. Set Status
        boolean isOnlinePayment = isOnlinePayment(request.paymentMethod());
        order.setStatus(isOnlinePayment ? OrderStatus.PENDING_PAYMENT : OrderStatus.PLACED);
        order.setPaymentStatus(isOnlinePayment ? "UNPAID" : "PENDING");

        orderRepository.save(order);

        // 6. Outbox for inventory/notification
        String payload = String.format(
                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"totalAmount\":%s,\"paymentMethod\":\"%s\",\"promoCode\":%s}",
                orderId,
                request.userId(),
                order.getStatus(),
                quote.grandTotal(),
                request.paymentMethod(),
                request.promoCode() == null || request.promoCode().isBlank() ? "null"
                        : "\"" + request.promoCode() + "\"");
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), OrderEventTypes.ORDER_CREATED, payload, "NEW"));

        return new CheckoutResponse(orderId, order.getStatus().name());
    }

    private double calculateShippingFee(String method) {
        if ("EXPRESS_2H".equalsIgnoreCase(method)) {
            return 45000.0;
        }
        return 15000.0; // Default STANDARD
    }

    private double calculateDiscount(String promoCode, double subtotal) {
        if ("PROMO10".equalsIgnoreCase(promoCode)) {
            return subtotal * 0.1;
        }
        return 0.0;
    }

    private boolean isOnlinePayment(String method) {
        return "CARD".equalsIgnoreCase(method) || "VNPAY".equalsIgnoreCase(method)
                || "ZALOPAY".equalsIgnoreCase(method);
    }

    private String generateOrderCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ORD-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
