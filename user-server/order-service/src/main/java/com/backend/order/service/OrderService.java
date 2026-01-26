package com.backend.order.service;

import com.backend.order.api.dto.*;
import com.backend.order.model.*;
import com.backend.order.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
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

    public OrderService(CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OutboxEventRepository outboxRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
    }

    public CartResponse upsertCartItem(UUID userId, CartItemRequest request) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        Cart cart = cartRepository.findById(userId).orElseGet(() -> new Cart(userId));
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
        List<CartItem> items = cartItemRepository.findByIdCartUserId(userId);
        List<CartResponseItem> responseItems = items.stream()
                .map(i -> new CartResponseItem(i.getId().getProductId(), i.getQuantity()))
                .toList();
        return new CartResponse(responseItems);
    }

    public CheckoutResponse checkout(CheckoutRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items required");
        }
        UUID orderId = UUID.randomUUID();
        double total = req.items().stream().mapToDouble(i -> i.unitPrice() * i.quantity()).sum();
        OrderEntity order = new OrderEntity(orderId, req.userId(), OrderStatus.PENDING_PAYMENT, total);
        orderRepository.save(order);

        List<OrderItem> orderItems = req.items().stream()
                .map(i -> new OrderItem(new OrderItemId(orderId, i.productId()), i.productName(), i.unitPrice(),
                        i.quantity()))
                .toList();
        orderItemRepository.saveAll(orderItems);

        // TODO: call inventory reserve; here we just assume success

        // Outbox OrderCreated (payload as simple json string)
        String payload = "{\"orderId\":\"" + orderId + "\",\"status\":\"PENDING_PAYMENT\"}";
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), "OrderCreated", payload, "NEW"));

        return new CheckoutResponse(orderId, order.getStatus().name());
    }

    public CheckoutResponse pay(PaymentRequest req) {
        OrderEntity order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        paymentRepository.save(new Payment(UUID.randomUUID(), order.getId(), req.provider(), PaymentStatus.PAID,
                req.amount(), req.transactionRef()));

        String payload = "{\"orderId\":\"" + order.getId() + "\",\"status\":\"PAID\"}";
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), "OrderPaid", payload, "NEW"));

        return new CheckoutResponse(order.getId(), order.getStatus().name());
    }

    public OrderResponse getOrder(UUID id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderItem> items = orderItemRepository.findByIdOrderId(id);
        List<OrderResponseItem> resItems = items.stream()
                .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(), i.getUnitPrice(),
                        i.getQuantity()))
                .collect(Collectors.toList());
        return new OrderResponse(order.getId(), order.getUserId(), order.getStatus().name(), order.getTotalAmount(),
                resItems);
    }

    public List<OrderResponse> listByUser(UUID userId) {
        List<OrderEntity> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByIdOrderId(order.getId());
            List<OrderResponseItem> resItems = items.stream()
                    .map(i -> new OrderResponseItem(i.getId().getProductId(), i.getProductName(), i.getUnitPrice(),
                            i.getQuantity()))
                    .collect(Collectors.toList());
            return new OrderResponse(order.getId(), order.getUserId(), order.getStatus().name(),
                    order.getTotalAmount(), resItems);
        }).toList();
    }
}
