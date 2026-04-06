package com.backend.order.service;

import com.backend.order.messaging.OrderEventTypes;
import com.backend.order.api.dto.*;
import com.backend.order.model.*;
import com.backend.order.repo.OrderRepository;
import com.backend.order.repo.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final DiscountClient discountClient;

    public CheckoutService(OrderRepository orderRepository, OutboxEventRepository outboxRepository,
            DiscountClient discountClient) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.discountClient = discountClient;
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
        QuoteDiscount quoteDiscount = resolveDiscount(request, subtotal, shippingFee);
        double discountAmount = quoteDiscount.discountAmount;
        double grandTotal = quoteDiscount.grandTotal;

        return new CheckoutQuoteResponse(subtotal, shippingFee, discountAmount, grandTotal);
    }

    @Transactional
    public CheckoutResponse placeOrder(CheckoutRequest request) {
        CheckoutQuoteResponse quote = computeQuote(request);
        UUID orderId = UUID.randomUUID();

        // If user submitted a promo code, enforce validity and record usage at the
        // discount-service.
        applyDiscountOrThrow(request, orderId, quote);

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
        order.setPaymentStatus("UNPAID");

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

    private record QuoteDiscount(double discountAmount, double grandTotal) {
    }

    private QuoteDiscount resolveDiscount(CheckoutRequest request, double subtotal, double shippingFee) {
        String promoCode = request.promoCode();
        if (promoCode == null || promoCode.isBlank()) {
            return new QuoteDiscount(0.0, subtotal + shippingFee);
        }

        try {
            BigDecimal subtotalBd = BigDecimal.valueOf(subtotal);
            BigDecimal shippingBd = BigDecimal.valueOf(shippingFee);
            BigDecimal totalBd = subtotalBd.add(shippingBd);

            DiscountClient.DiscountApplyResponse res = discountClient.validate(
                    request.userId(),
                    promoCode,
                    "quote-" + UUID.randomUUID(),
                    subtotalBd,
                    shippingBd,
                    totalBd);

            if (res == null || res.valid() == null || !res.valid()) {
                return new QuoteDiscount(0.0, subtotal + shippingFee);
            }

            BigDecimal discountTotal = safe(res.discountAmount()).add(safe(res.shippingDiscount()));
            BigDecimal finalTotal = res.finalTotal() == null ? totalBd.subtract(discountTotal) : res.finalTotal();

            return new QuoteDiscount(discountTotal.doubleValue(), finalTotal.doubleValue());
        } catch (Exception ex) {
            // Resilience: do not break quote on discount-service issues.
            return new QuoteDiscount(0.0, subtotal + shippingFee);
        }
    }

    private void applyDiscountOrThrow(CheckoutRequest request, UUID orderId, CheckoutQuoteResponse quote) {
        String promoCode = request.promoCode();
        if (promoCode == null || promoCode.isBlank()) {
            return;
        }

        try {
            BigDecimal subtotalBd = BigDecimal.valueOf(quote.subtotal());
            BigDecimal shippingBd = BigDecimal.valueOf(quote.shippingFee());
            BigDecimal totalBd = subtotalBd.add(shippingBd);

            DiscountClient.DiscountApplyResponse res = discountClient.apply(
                    request.userId(),
                    promoCode,
                    orderId.toString(),
                    subtotalBd,
                    shippingBd,
                    totalBd);

            if (res == null || res.valid() == null || !res.valid()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        res != null && res.reason() != null && !res.reason().isBlank()
                                ? res.reason()
                                : "Invalid promo code");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            // If the user explicitly provided a promo code, fail fast so the UI can prompt
            // retry.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Discount service unavailable. Please try again.");
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
