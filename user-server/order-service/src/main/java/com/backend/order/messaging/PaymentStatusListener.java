package com.backend.order.messaging;

import com.backend.order.model.OrderEntity;
import com.backend.order.model.OrderStatus;
import com.backend.order.model.Payment;
import com.backend.order.model.PaymentStatus;
import com.backend.order.repo.OrderRepository;
import com.backend.order.repo.PaymentRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentStatusListener {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public PaymentStatusListener(OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    @KafkaListener(topics = "payment.status", groupId = "order-payment-group")
    public void handlePaymentStatus(PaymentStatusEvent event) {
        UUID orderId = UUID.fromString(event.orderId());
        OrderEntity order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            // Log and ignore or handle error
            return;
        }

        // 1. Idempotency: skip if already PAID or COMPLETED
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.COMPLETED) {
            return;
        }

        if ("PAID".equals(event.status())) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus("PAID");

            // Record payment for audit
            Payment payment = new Payment();
            payment.setId(UUID.randomUUID());
            payment.setOrderId(orderId);
            payment.setProvider(event.provider());
            payment.setStatus(PaymentStatus.PAID);
            payment.setAmount(order.getTotalAmount());
            payment.setTransactionRef(event.transactionRef());
            paymentRepository.save(payment);
        } else {
            order.setPaymentStatus("UNPAID");
        }

        orderRepository.save(order);
    }
}
