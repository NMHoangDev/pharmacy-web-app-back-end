package com.pharmacy.discount.repository;

import com.pharmacy.discount.entity.Discount;
import com.pharmacy.discount.entity.DiscountStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    List<Discount> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            DiscountStatus status,
            LocalDateTime start,
            LocalDateTime end);

    List<Discount> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateAsc(
            DiscountStatus status,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);
}
