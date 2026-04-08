package com.pharmacy.discount.repository;

import com.pharmacy.discount.entity.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountUsageRepository extends JpaRepository<DiscountUsage, Long> {
    long countByDiscount_IdAndUserId(Long discountId, String userId);
}
