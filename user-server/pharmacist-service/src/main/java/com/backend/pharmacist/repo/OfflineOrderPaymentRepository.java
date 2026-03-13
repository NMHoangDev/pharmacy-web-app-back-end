package com.backend.pharmacist.repo;

import com.backend.pharmacist.model.OfflineOrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfflineOrderPaymentRepository extends JpaRepository<OfflineOrderPayment, UUID> {
    List<OfflineOrderPayment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
