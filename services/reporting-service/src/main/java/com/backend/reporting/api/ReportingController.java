package com.backend.reporting.api;

import com.backend.reporting.model.MetricCounter;
import com.backend.reporting.service.MetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reporting")
@RequiredArgsConstructor
public class ReportingController {

    private final MetricService metricService;

    @GetMapping("/metrics")
    public List<MetricCounter> listMetrics(@RequestParam(required = false) String category) {
        return metricService.listByCategory(category);
    }
}
