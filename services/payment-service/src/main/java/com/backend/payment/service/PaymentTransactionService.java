package com.backend.payment.service;

import com.backend.payment.model.PaymentProvider;
import com.backend.payment.model.PaymentStatus;
import com.backend.payment.model.PaymentTransaction;
import com.backend.payment.repo.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentTransactionService {
    private final PaymentTransactionRepository repository;

    public PaymentTransactionService(PaymentTransactionRepository repository) {
        this.repository = repository;
    }

    public PaymentTransaction createPending(String orderId, PaymentProvider provider, String txnRef, long amount,
            String currency) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(UUID.randomUUID());
        tx.setOrderId(orderId);
        tx.setProvider(provider);
        tx.setTxnRef(txnRef);
        tx.setAmount(amount);
        tx.setCurrency(currency == null || currency.isBlank() ? "VND" : currency);
        tx.setStatus(PaymentStatus.PENDING);
        return repository.save(tx);
    }

    public Optional<PaymentTransaction> findByTxnRef(String txnRef) {
        return repository.findByTxnRef(txnRef);
    }

    public boolean existsByTxnRef(String txnRef) {
        return repository.existsByTxnRef(txnRef);
    }

    @Transactional
    public Optional<PaymentTransaction> updateIfPending(String txnRef, PaymentStatus status, String responseCode,
            String transactionStatus, String gatewayTransactionNo, String payDate, String rawCallback,
            Instant paidAt) {
        Optional<PaymentTransaction> optional = repository.findByTxnRef(txnRef);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        PaymentTransaction tx = optional.get();
        if (tx.getStatus() != PaymentStatus.PENDING) {
            return Optional.empty();
        }
        tx.setStatus(status);
        tx.setResponseCode(responseCode);
        tx.setTransactionStatus(transactionStatus);
        tx.setGatewayTransactionNo(gatewayTransactionNo);
        tx.setPayDate(payDate);
        tx.setRawCallback(rawCallback);
        if (status == PaymentStatus.SUCCEEDED && paidAt != null) {
            tx.setPaidAt(paidAt);
        }
        return Optional.of(repository.save(tx));
    }
}
