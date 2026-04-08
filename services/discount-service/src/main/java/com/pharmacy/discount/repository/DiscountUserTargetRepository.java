package com.pharmacy.discount.repository;

import com.pharmacy.discount.entity.DiscountUserTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountUserTargetRepository extends JpaRepository<DiscountUserTarget, Long> {
    boolean existsByDiscount_Id(Long discountId);

    boolean existsByDiscount_IdAndUserId(Long discountId, String userId);

    List<DiscountUserTarget> findByDiscount_Id(Long discountId);

    void deleteByDiscount_Id(Long discountId);
}
