package com.backend.payment.repo;

import com.backend.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);
}
