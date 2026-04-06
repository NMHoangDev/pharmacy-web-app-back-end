package com.pharmacy.discount.repository;

import com.pharmacy.discount.entity.DiscountScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountScopeRepository extends JpaRepository<DiscountScope, Long> {
    List<DiscountScope> findByDiscount_Id(Long discountId);

    void deleteByDiscount_Id(Long discountId);
}
