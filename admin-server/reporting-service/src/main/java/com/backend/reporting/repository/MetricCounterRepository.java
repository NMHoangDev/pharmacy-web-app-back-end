package com.backend.reporting.repository;

import com.backend.reporting.model.MetricCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricCounterRepository extends JpaRepository<MetricCounter, Long> {
    List<MetricCounter> findByCategory(String category);

    Optional<MetricCounter> findByCategoryAndKey(String category, String key);
}
