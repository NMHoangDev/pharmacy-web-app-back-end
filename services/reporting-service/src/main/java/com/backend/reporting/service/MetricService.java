package com.backend.reporting.service;

import com.backend.reporting.model.MetricCounter;
import com.backend.reporting.repository.MetricCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MetricService {

    private final MetricCounterRepository repository;

    public List<MetricCounter> listByCategory(String category) {
        return category == null ? repository.findAll() : repository.findByCategory(category);
    }

    public MetricCounter increment(String category, String key) {
        MetricCounter counter = repository.findByCategoryAndKey(category, key)
                .orElseGet(() -> {
                    MetricCounter c = new MetricCounter();
                    c.setCategory(category);
                    c.setKey(key);
                    return c;
                });
        counter.setCount(counter.getCount() == null ? 1 : counter.getCount() + 1);
        counter.setLastEventAt(LocalDateTime.now());
        return repository.save(counter);
    }
}
