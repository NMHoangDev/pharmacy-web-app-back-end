package com.backend.inventory.api;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.model.StockDocumentStatus;
import com.backend.inventory.model.StockDocumentType;
import com.backend.inventory.service.InventoryReportService;
import com.backend.inventory.service.StockDocumentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/admin")
public class InventoryAdminApi {

    private final StockDocumentService stockDocumentService;
    private final InventoryReportService inventoryReportService;

    public InventoryAdminApi(StockDocumentService stockDocumentService,
            InventoryReportService inventoryReportService) {
        this.stockDocumentService = stockDocumentService;
        this.inventoryReportService = inventoryReportService;
    }

    @PostMapping("/stock-documents")
    public ResponseEntity<StockDocumentResponse> create(@RequestBody @Valid StockDocumentCreateRequest request) {
        return ResponseEntity.ok(stockDocumentService.createDraft(request));
    }

    @PostMapping("/stock-documents/{id}/submit")
    public ResponseEntity<StockDocumentResponse> submit(@PathVariable UUID id,
            @RequestBody(required = false) StockDocumentSubmitRequest request) {
        return ResponseEntity.ok(stockDocumentService.submit(id, request));
    }

    @PostMapping("/stock-documents/{id}/approve")
    public ResponseEntity<StockDocumentResponse> approve(@PathVariable UUID id,
            @RequestBody(required = false) StockDocumentApproveRequest request) {
        return ResponseEntity.ok(stockDocumentService.approve(id, request));
    }

    @PostMapping("/stock-documents/{id}/reject")
    public ResponseEntity<StockDocumentResponse> reject(@PathVariable UUID id,
            @RequestBody(required = false) StockDocumentRejectRequest request) {
        return ResponseEntity.ok(stockDocumentService.reject(id, request));
    }

    @GetMapping("/stock-documents/{id}")
    public ResponseEntity<StockDocumentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(stockDocumentService.get(id));
    }

    @GetMapping("/stock-documents")
    public ResponseEntity<StockDocumentPageResponse> list(
            @RequestParam(name = "type", required = false) StockDocumentType type,
            @RequestParam(name = "status", required = false) StockDocumentStatus status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 20 : size;
        return ResponseEntity
                .ok(stockDocumentService.list(type, status, branchId, from, to, keyword, safePage, safeSize));
    }

    @GetMapping("/reports/stock/export")
    public ResponseEntity<byte[]> exportStockReport(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "branchId") UUID branchId) {
        InventoryReportService.ReportFile report = inventoryReportService.exportStockReport(q, categoryId, status,
                branchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report.content());
    }
}
