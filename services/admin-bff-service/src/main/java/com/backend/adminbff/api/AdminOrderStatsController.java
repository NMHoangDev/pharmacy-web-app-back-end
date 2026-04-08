package com.backend.adminbff.api;

import com.backend.adminbff.dto.stats.AdminStatsEnvelope;
import com.backend.adminbff.dto.stats.OrdersStatsResponse;
import com.backend.adminbff.service.AdminOrderStatsService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderStatsController {

    private final AdminOrderStatsService service;

    public AdminOrderStatsController(AdminOrderStatsService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsEnvelope<OrdersStatsResponse>> stats(
            @RequestParam(defaultValue = "7d") String range) {
        OrdersStatsResponse metrics = service.getStats(range);
        return ResponseEntity.ok(new AdminStatsEnvelope<>(metrics, Instant.now(), range));
    }
}
