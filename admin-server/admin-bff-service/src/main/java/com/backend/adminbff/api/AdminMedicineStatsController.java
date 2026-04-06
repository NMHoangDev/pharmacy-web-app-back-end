package com.backend.adminbff.api;

import com.backend.adminbff.dto.stats.AdminStatsEnvelope;
import com.backend.adminbff.dto.stats.MedicinesStatsResponse;
import com.backend.adminbff.service.AdminMedicineStatsService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/medicines")
public class AdminMedicineStatsController {

    private final AdminMedicineStatsService service;

    public AdminMedicineStatsController(AdminMedicineStatsService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsEnvelope<MedicinesStatsResponse>> stats(
            @RequestParam(defaultValue = "7d") String range) {
        MedicinesStatsResponse metrics = service.getStats(range);
        return ResponseEntity.ok(new AdminStatsEnvelope<>(metrics, Instant.now(), range));
    }
}
