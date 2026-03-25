package com.backend.order.service;

import com.backend.order.messaging.OrderEventTypes;
import com.backend.order.api.dto.*;
import com.backend.order.model.*;
import com.backend.order.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        private final BranchClient branchClient;
        private final InventoryClient inventoryClient;

        public OrderService(CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        PaymentRepository paymentRepository,
                        OutboxEventRepository outboxRepository,
                        CheckoutService checkoutService,
                        BranchClient branchClient,
                        InventoryClient inventoryClient) {
                this.cartRepository = cartRepository;
                this.cartItemRepository = cartItemRepository;
                this.orderRepository = orderRepository;
                this.orderItemRepository = orderItemRepository;
                this.paymentRepository = paymentRepository;
                this.outboxRepository = outboxRepository;
                this.checkoutService = checkoutService;
                this.branchClient = branchClient;
                this.inventoryClient = inventoryClient;
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

                String cartPayload = String.format(
                                "{\"userId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d}",
                                userId,
                                request.productId(),
                                request.quantity());
                outboxRepository.save(new OutboxEvent(UUID.randomUUID(), OrderEventTypes.CART_ITEM_ADDED, cartPayload,
                                "NEW"));
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
                order.setOrderCode(generateOrderCode());
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

                String payload = String.format(
                                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"totalAmount\":%s,\"paymentMethod\":\"%s\"}",
                                orderId,
                                order.getUserId(),
                                order.getStatus(),
                                order.getTotalAmount(),
                                order.getPaymentMethod());
                outboxRepository.save(
                                new OutboxEvent(UUID.randomUUID(), OrderEventTypes.ORDER_CREATED, payload, "NEW"));

                return new CheckoutResponse(orderId, order.getStatus().name());
        }

        public CheckoutResponse pay(PaymentRequest req) {
                OrderEntity order = orderRepository.findById(requireId(req.orderId(), "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));

                // Idempotency: if already paid/confirmed, do not create duplicate payment
                // records/events.
                if (PaymentStatus.PAID.name().equalsIgnoreCase(order.getPaymentStatus())
                                || order.getStatus() == OrderStatus.CONFIRMED
                                || order.getStatus() == OrderStatus.COMPLETED) {
                        return new CheckoutResponse(order.getId(), order.getStatus().name());
                }

                order.setStatus(OrderStatus.CONFIRMED);
                order.setUpdatedAt(Instant.now());
                order.setPaymentMethod(req.provider());
                order.setPaymentStatus(PaymentStatus.PAID.name());
                orderRepository.save(order);

                paymentRepository.save(new Payment(UUID.randomUUID(), order.getId(), req.provider(), PaymentStatus.PAID,
                                req.amount(), req.transactionRef()));

                String payload = String.format(
                                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"totalAmount\":%s,\"paymentMethod\":\"%s\",\"paymentStatus\":\"%s\"}",
                                order.getId(),
                                order.getUserId(),
                                order.getStatus(),
                                order.getTotalAmount(),
                                order.getPaymentMethod(),
                                order.getPaymentStatus());
                outboxRepository.save(new OutboxEvent(UUID.randomUUID(), OrderEventTypes.ORDER_PAID, payload, "NEW"));

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

                String payload = String.format(
                                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"totalAmount\":%s}",
                                order.getId(),
                                order.getUserId(),
                                order.getStatus(),
                                order.getTotalAmount());
                outboxRepository.save(
                                new OutboxEvent(UUID.randomUUID(), OrderEventTypes.ORDER_STATUS_UPDATED, payload,
                                                "NEW"));

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

                String payload = String.format(
                                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"status\":\"%s\",\"totalAmount\":%s}",
                                order.getId(),
                                order.getUserId(),
                                order.getStatus(),
                                order.getTotalAmount());
                outboxRepository.save(
                                new OutboxEvent(UUID.randomUUID(), OrderEventTypes.ORDER_CANCELLED, payload, "NEW"));

                List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
                List<OrderResponseItem> resItems = items.stream()
                                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                i.getUnitPrice(),
                                                i.getQuantity()))
                                .collect(Collectors.toList());
                return toOrderResponse(order, resItems);
        }

        public BranchAvailabilityResponse getBranchAvailability(UUID orderId) {
                OrderEntity order = orderRepository.findById(requireId(orderId, "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
                if (items.isEmpty()) {
                        return new BranchAvailabilityResponse(orderId, List.of(), List.of(), List.of());
                }

                List<BranchClient.BranchSummaryDto> branches = branchClient.listActive();
                List<BranchSummaryDto> branchDtos = branches.stream()
                                .map(b -> new BranchSummaryDto(b.id(), b.code(), b.name(), b.status(), b.addressLine(),
                                                b.ward(), b.district(), b.city(), b.province(), b.country(), b.phone(),
                                                b.timezone()))
                                .toList();

                List<UUID> branchIds = branchDtos.stream().map(BranchSummaryDto::id).toList();
                List<InventoryClient.ItemQuantity> reqItems = items.stream()
                                .map(i -> new InventoryClient.ItemQuantity(i.getId().getProductId(),
                                                i.getQuantity()))
                                .toList();

                InventoryClient.AvailabilityBatchResponse availability = inventoryClient
                                .batchAvailability(new InventoryClient.AvailabilityBatchRequest(
                                                branchIds,
                                                reqItems));
                List<InventoryClient.AvailabilityBatchItem> availabilityItems = availability == null
                                || availability.items() == null ? List.of() : availability.items();

                List<BranchAvailabilityItem> responseItems = items.stream().map(i -> {
                        InventoryClient.AvailabilityBatchItem found = availabilityItems.stream()
                                        .filter(a -> a.productId().equals(i.getId().getProductId()))
                                        .findFirst()
                                        .orElse(null);
                        List<BranchStockAvailability> byBranch = found == null || found.byBranch() == null
                                        ? List.of()
                                        : found.byBranch().stream()
                                                        .map(b -> new BranchStockAvailability(b.branchId(),
                                                                        b.available(), b.onHand(), b.reserved()))
                                                        .toList();
                        return new BranchAvailabilityItem(i.getId().getProductId(), i.getProductName(),
                                        i.getQuantity(), byBranch);
                }).toList();

                List<UUID> recommended = branchIds.stream()
                                .filter(branchId -> responseItems.stream().allMatch(item -> item.byBranch().stream()
                                                .filter(b -> b.branchId().equals(branchId))
                                                .findFirst()
                                                .map(b -> b.availableQty() >= item.quantityOrdered())
                                                .orElse(false)))
                                .toList();

                return new BranchAvailabilityResponse(orderId, responseItems, branchDtos, recommended);
        }

        public OrderResponse assignBranch(UUID orderId, AssignBranchRequest request) {
                if (request == null || request.branchId() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId required");
                }
                OrderEntity order = orderRepository.findById(requireId(orderId, "orderId required"))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Order not found"));
                if (!isAssignableStatus(order.getStatus())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Order status not allowed for assignment");
                }

                BranchClient.BranchSummaryDto branch = branchClient.getBranch(request.branchId());
                if (branch == null || branch.status() == null
                                || !branch.status().equalsIgnoreCase("ACTIVE")) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is not active");
                }

                if (order.getFulfillmentBranchId() != null
                                && !order.getFulfillmentBranchId().equals(request.branchId())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                        "Order already assigned to another branch");
                }

                List<OrderItem> items = orderItemRepository.findByIdOrderId(orderId);
                List<InventoryClient.ItemQuantity> reqItems = items.stream()
                                .map(i -> new InventoryClient.ItemQuantity(i.getId().getProductId(),
                                                i.getQuantity()))
                                .toList();

                InventoryClient.AvailabilityBatchResponse availability = inventoryClient
                                .batchAvailability(new InventoryClient.AvailabilityBatchRequest(
                                                List.of(request.branchId()), reqItems));
                List<InventoryClient.AvailabilityBatchItem> availabilityItems = availability == null
                                || availability.items() == null ? List.of() : availability.items();
                for (OrderItem item : items) {
                        InventoryClient.AvailabilityBatchItem found = availabilityItems.stream()
                                        .filter(a -> a.productId().equals(item.getId().getProductId()))
                                        .findFirst()
                                        .orElse(null);
                        InventoryClient.AvailabilityByBranch stock = found == null || found.byBranch() == null
                                        ? null
                                        : found.byBranch().stream()
                                                        .filter(b -> b.branchId().equals(request.branchId()))
                                                        .findFirst()
                                                        .orElse(null);
                        int available = stock == null ? 0 : stock.available();
                        if (available < item.getQuantity()) {
                                throw new ResponseStatusException(HttpStatus.CONFLICT,
                                                "Insufficient inventory for product " + item.getId().getProductId());
                        }
                }

                InventoryClient.ReserveResponse reserve = inventoryClient.reserve(
                                new InventoryClient.ReserveRequest(orderId, reqItems, 3600,
                                                "order assignment", "admin", request.branchId()));
                UUID reservationId = reserve == null ? null : reserve.reservationId();

                order.setFulfillmentBranchId(request.branchId());
                order.setFulfillmentAssignedAt(Instant.now());
                order.setFulfillmentAssignedBy(null);
                order.setFulfillmentStatus("ASSIGNED");
                order.setInventoryReservationId(reservationId);
                orderRepository.save(order);

                List<OrderResponseItem> resItems = items.stream()
                                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(),
                                                i.getUnitPrice(), i.getQuantity()))
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
                                order.getFulfillmentBranchId(),
                                order.getFulfillmentAssignedAt(),
                                order.getFulfillmentAssignedBy(),
                                order.getFulfillmentStatus(),
                                order.getInventoryReservationId(),
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

        private boolean isAssignableStatus(OrderStatus status) {
                return status == OrderStatus.PENDING_PAYMENT
                                || status == OrderStatus.PLACED
                                || status == OrderStatus.CONFIRMED;
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

        private String generateOrderCode() {
                String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
                return "ORD-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
}
