package com.backend.order.service;

import com.backend.order.api.dto.*;
import com.backend.order.model.*;
import com.backend.order.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

        private final CartRepository cartRepository;
        private final CartItemRepository cartItemRepository;
        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final PaymentRepository paymentRepository;
        private final OutboxEventRepository outboxRepository;
        private final CheckoutService checkoutService;

        public OrderService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        OutboxEventRepository outboxRepository,
                        CheckoutService checkoutService) {
                this.cartRepository = cartRepository;
                this.cartItemRepository = cartItemRepository;
                this.orderRepository = orderRepository;
                this.orderItemRepository = orderItemRepository;
                this.paymentRepository = paymentRepository;
                this.outboxRepository = outboxRepository;
                this.checkoutService = checkoutService;
        }

        public CartResponse upsertCartItem(UUID userId, CartItemRequest request) {
                if (userId == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
                }
                Cart cart = cartRepository.findById(userId).orElseGet(() -> new Cart(userId));
                UUID requestedBranchId = request.branchId();
                if (requestedBranchId != null) {
                        if (cart.getBranchId() != null && !cart.getBranchId().equals(requestedBranchId)) {
                                cartItemRepository.deleteByIdCartUserId(userId);
                        }
                        cart.setBranchId(requestedBranchId);
                }
                cart.setUpdatedAt(Instant.now());
                cartRepository.save(cart);

                CartItemId id = new CartItemId(userId, request.productId());
                CartItem item = cartItemRepository.findById(id).orElse(new CartItem(id, request.quantity()));
                item.setQuantity(request.quantity());
                cartItemRepository.save(item);
                return getCart(userId);
        }

        public CartResponse removeCartItem(UUID userId, UUID productId) {
                CartItemId id = new CartItemId(userId, productId);
                cartItemRepository.deleteById(id);
                return getCart(userId);
        }

        public CartResponse getCart(UUID userId) {
                UUID safeUserId = requireId(userId, "userId required");
                List<CartItem> items = cartItemRepository.findByIdCartUserId(safeUserId);
                List<CartResponseItem> responseItems = items.stream()
                                .map(i -> new CartResponseItem(i.getId().getProductId(), i.getQuantity()))
                                .toList();
                UUID branchId = cartRepository.findById(safeUserId).map(Cart::getBranchId).orElse(null);
                return new CartResponse(branchId, responseItems);
        }

        public CheckoutResponse checkout(CheckoutRequest req) {
                if (req.items() == null || req.items().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items required");
                }
                UUID orderId = UUID.randomUUID();
                CheckoutQuoteResponse quote = checkoutService.computeQuote(req);
                UUID resolvedBranchId = resolveBranchId(req);
                OrderEntity order = new OrderEntity();
                order.setId(orderId);
                order.setUserId(req.userId());
                order.setBranchId(resolvedBranchId);
                order.setSubtotal(quote.subtotal());
                order.setShippingFee(quote.shippingFee());
                order.setDiscountAmount(quote.discountAmount());
                order.setTotalAmount(quote.grandTotal());
                order.setPaymentMethod(req.paymentMethod());
                order.setNote(req.note());

                if (req.shippingAddress() != null) {
                        OrderShippingAddress address = new OrderShippingAddress();
                        address.setId(UUID.randomUUID());
                        address.setFullName(req.shippingAddress().fullName());
                        address.setPhone(req.shippingAddress().phone());
                        address.setAddressLine(req.shippingAddress().addressLine());
                        address.setProvinceName(req.shippingAddress().provinceName());
                        address.setProvinceCode(req.shippingAddress().provinceCode());
                        address.setDistrictName(req.shippingAddress().districtName());
                        address.setDistrictCode(req.shippingAddress().districtCode());
                        address.setWardName(req.shippingAddress().wardName());
                        address.setWardCode(req.shippingAddress().wardCode());
                        order.setShippingAddress(address);
                }

                if (req.shippingMethod() != null && !req.shippingMethod().isBlank()) {
                        OrderShipping shipping = new OrderShipping();
                        shipping.setId(UUID.randomUUID());
                        shipping.setMethod(req.shippingMethod());
                        shipping.setFee(quote.shippingFee());
                        shipping.setEtaRange(req.shippingMethod().equalsIgnoreCase("EXPRESS_2H") ? "2 hours"
                                        : "1-3 days");
                        order.setShipping(shipping);
                }

                if (req.promoCode() != null && !req.promoCode().isBlank()) {
                        OrderDiscount discount = new OrderDiscount();
                        discount.setId(UUID.randomUUID());
                        discount.setPromoCode(req.promoCode());
                        discount.setDiscountAmount(quote.discountAmount());
                        discount.setDescription("Applied promo code: " + req.promoCode());
                        order.setDiscount(discount);
                }

                boolean isOnlinePayment = isOnlinePayment(req.paymentMethod());
                order.setStatus(isOnlinePayment ? OrderStatus.PENDING_PAYMENT : OrderStatus.PLACED);
                order.setPaymentStatus(isOnlinePayment ? "UNPAID" : "PENDING");
                orderRepository.save(order);

                List<OrderItem> orderItems = Objects.requireNonNull(req.items(), "items required").stream()
                                .map(i -> new OrderItem(new OrderItemId(orderId, i.productId()), i.productName(),
                                                i.unitPrice(),
                                                i.quantity()))
                                .toList();
                orderItemRepository.saveAll(requireItems(orderItems));

                // TODO: call inventory reserve; here we just assume success

                // Outbox OrderCreated (payload as simple json string)
                String payload = "{\"orderId\":\"" + orderId + "\",\"status\":\"PENDING_PAYMENT\"}";
                outboxRepository.save(new OutboxEvent(UUID.randomUUID(), "OrderCreated", payload, "NEW"));

                return new CheckoutResponse(orderId, order.getStatus().name());
        }

        public CheckoutResponse pay(PaymentRequest req) {
                OrderEntity order = orderRepository.findById(requireId(req.orderId(), "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(Instant.now());
                order.setPaymentMethod(req.provider());
                order.setPaymentStatus(PaymentStatus.PAID.name());
                orderRepository.save(order);

                paymentRepository.save(new Payment(UUID.randomUUID(), order.getId(), req.provider(), PaymentStatus.PAID,
                                req.amount(), req.transactionRef()));

                String payload = "{\"orderId\":\"" + order.getId() + "\",\"status\":\"CONFIRMED\"}";
                outboxRepository.save(new OutboxEvent(UUID.randomUUID(), "OrderPaid", payload, "NEW"));

                return new CheckoutResponse(order.getId(), order.getStatus().name());
        }

        public OrderResponse getOrder(UUID id) {
                OrderEntity order = orderRepository.findById(requireId(id, "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                List<OrderItem> items = orderItemRepository.findByIdOrderId(id);
                List<OrderResponseItem> resItems = items.stream()
                                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                i.getUnitPrice(),
                                                i.getQuantity()))
                                .collect(Collectors.toList());
                return toOrderResponse(order, resItems);
        }

        public List<OrderResponse> listByUser(UUID userId) {
                List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                return orders.stream().map(order -> {
                        List<OrderItem> items = orderItemRepository.findByIdOrderId(order.getId());
                        List<OrderResponseItem> resItems = items.stream()
                                        .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                        i.getUnitPrice(),
                                                        i.getQuantity()))
                                        .collect(Collectors.toList());
                        return toOrderResponse(order, resItems);
                }).toList();
        }

        public List<OrderResponse> listByUser(UUID userId, UUID branchId) {
                if (branchId == null) {
                        return listByUser(userId);
                }
                List<OrderEntity> orders = orderRepository.findByUserIdAndBranchIdOrderByCreatedAtDesc(userId,
                                branchId);
                return orders.stream().map(order -> {
                        List<OrderItem> items = orderItemRepository.findByIdOrderId(order.getId());
                        List<OrderResponseItem> resItems = items.stream()
                                        .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                        i.getUnitPrice(),
                                                        i.getQuantity()))
                                        .collect(Collectors.toList());
                        return toOrderResponse(order, resItems);
                }).toList();
        }

        public List<OrderResponse> listAll(UUID userId, String status) {
                return listAll(userId, status, null);
        }

        public List<OrderResponse> listAll(UUID userId, String status, UUID branchId) {
                if (userId != null) {
                        return listByUser(userId, branchId);
                }

                List<OrderEntity> orders;
                if (status != null && !status.isBlank()) {
                        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                        orders = branchId == null
                                        ? orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus)
                                        : orderRepository.findByStatusAndBranchIdOrderByCreatedAtDesc(orderStatus,
                                                        branchId);
                } else {
                        orders = branchId == null
                                        ? orderRepository.findAllByOrderByCreatedAtDesc()
                                        : orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
                }

                return orders.stream().map(order -> {
                        List<OrderItem> items = orderItemRepository.findByIdOrderId(order.getId());
                        List<OrderResponseItem> resItems = items.stream()
                                        .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                        i.getUnitPrice(),
                                                        i.getQuantity()))
                                        .collect(Collectors.toList());
                        return toOrderResponse(order, resItems);
                }).toList();
        }

        public OrderResponse updateOrderStatus(UUID orderId, String status) {
                if (status == null || status.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status required");
                }
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                OrderEntity order = orderRepository.findById(requireId(orderId, "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                order.setStatus(orderStatus);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);

                List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
                List<OrderResponseItem> resItems = items.stream()
                                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                i.getUnitPrice(),
                                                i.getQuantity()))
                                .collect(Collectors.toList());
                return toOrderResponse(order, resItems);
        }

        public OrderResponse cancelOrder(UUID orderId) {
                OrderEntity order = orderRepository.findById(requireId(orderId, "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                order.setStatus(OrderStatus.CANCELED);
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);

                List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
                List<OrderResponseItem> resItems = items.stream()
                                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                i.getUnitPrice(),
                                                i.getQuantity()))
                                .collect(Collectors.toList());
                return toOrderResponse(order, resItems);
        }

        private OrderResponse toOrderResponse(OrderEntity order, List<OrderResponseItem> items) {
                ShippingAddressResponse shippingAddress = null;
                if (order.getShippingAddress() != null) {
                        OrderShippingAddress address = order.getShippingAddress();
                        shippingAddress = new ShippingAddressResponse(
                                        address.getFullName(),
                                        address.getPhone(),
                                        address.getAddressLine(),
                                        address.getProvinceName(),
                                        address.getProvinceCode(),
                                        address.getDistrictName(),
                                        address.getDistrictCode(),
                                        address.getWardName(),
                                        address.getWardCode());
                }

                ShippingResponse shipping = null;
                if (order.getShipping() != null) {
                        OrderShipping orderShipping = order.getShipping();
                        shipping = new ShippingResponse(
                                        orderShipping.getMethod(),
                                        orderShipping.getFee(),
                                        orderShipping.getEtaRange());
                }

                DiscountResponse discount = null;
                if (order.getDiscount() != null) {
                        OrderDiscount orderDiscount = order.getDiscount();
                        discount = new DiscountResponse(
                                        orderDiscount.getPromoCode(),
                                        orderDiscount.getDiscountAmount(),
                                        orderDiscount.getDescription());
                }

                return new OrderResponse(
                                order.getId(),
                                order.getUserId(),
                                order.getBranchId(),
                                order.getStatus().name(),
                                order.getCreatedAt(),
                                order.getSubtotal(),
                                order.getShippingFee(),
                                order.getDiscountAmount(),
                                order.getTotalAmount(),
                                order.getPaymentMethod(),
                                order.getPaymentStatus(),
                                order.getNote(),
                                shippingAddress,
                                shipping,
                                discount,
                                items);
        }

        private UUID resolveBranchId(CheckoutRequest request) {
                if (request.branchId() != null) {
                        return request.branchId();
                }
                if (request.userId() == null) {
                        return null;
                }
                return cartRepository.findById(requireId(request.userId(), "userId required"))
                                .map(Cart::getBranchId)
                                .orElse(null);
        }

        private @NonNull UUID requireId(UUID id, String message) {
                return Objects.requireNonNull(id, message);
        }

        private @NonNull Iterable<OrderItem> requireItems(List<OrderItem> items) {
                return Objects.requireNonNull(items, "items required");
        }

        private boolean isOnlinePayment(String method) {
                return "CARD".equalsIgnoreCase(method) || "VNPAY".equalsIgnoreCase(method)
                                || "ZALOPAY".equalsIgnoreCase(method);
        }
}
