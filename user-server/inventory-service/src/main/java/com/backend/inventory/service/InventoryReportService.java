package com.backend.inventory.service;

import com.backend.inventory.model.InventoryItem;
import com.backend.inventory.repo.InventoryItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InventoryReportService {

    private static final int MAX_ROWS = 50000;
    private static final int INVENTORY_BATCH_SIZE = 500;

    private final CatalogClient catalogClient;
    private final InventoryItemRepository inventoryItemRepository;
    private final BranchClient branchClient;
    private final ObjectMapper objectMapper;

    public InventoryReportService(CatalogClient catalogClient, InventoryItemRepository inventoryItemRepository,
            BranchClient branchClient, ObjectMapper objectMapper) {
        this.catalogClient = catalogClient;
        this.inventoryItemRepository = inventoryItemRepository;
        this.branchClient = branchClient;
        this.objectMapper = objectMapper;
    }

    public ReportFile exportStockReport(String q, UUID categoryId, String status, UUID branchId) {
        if (branchId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
        }

        List<CatalogClient.CatalogProductDto> products = catalogClient.fetchAllProducts(q, categoryId, branchId);
        if (products.size() > MAX_ROWS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many rows to export");
        }

        Map<UUID, String> categoryMap = catalogClient.listCategories().stream()
                .collect(Collectors.toMap(CatalogClient.CatalogCategoryDto::id, CatalogClient.CatalogCategoryDto::name,
                        (a, b) -> a));

        Map<UUID, InventoryItem> inventoryMap = loadInventory(branchId, products);
        String statusFilter = normalizeStatusFilter(status);
        List<ReportRow> rows = new ArrayList<>();

        for (CatalogClient.CatalogProductDto product : products) {
            InventoryItem inventory = inventoryMap.get(product.id());
            int onHand = inventory == null ? 0 : inventory.getOnHand();
            int reserved = inventory == null ? 0 : inventory.getReserved();
            int available = Math.max(onHand - reserved, 0);
            Attributes attrs = parseAttributes(product.attributes());
            int threshold = attrs.threshold;
            String stockStatus = resolveStockStatus(onHand, threshold);
            if (statusFilter != null && !statusFilter.equals(stockStatus)) {
                continue;
            }

            rows.add(new ReportRow(product, categoryMap.get(product.categoryId()), onHand, reserved, available,
                    threshold, stockStatus, attrs.unit));
        }

        byte[] content = buildWorkbook(rows, branchId);
        return new ReportFile(buildFileName(branchId), content);
    }

    private Map<UUID, InventoryItem> loadInventory(UUID branchId,
            List<CatalogClient.CatalogProductDto> products) {
        if (products.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, InventoryItem> map = new HashMap<>();
        List<UUID> ids = products.stream().map(CatalogClient.CatalogProductDto::id).toList();
        for (int i = 0; i < ids.size(); i += INVENTORY_BATCH_SIZE) {
            int end = Math.min(i + INVENTORY_BATCH_SIZE, ids.size());
            List<UUID> batch = ids.subList(i, end);
            List<InventoryItem> items = inventoryItemRepository.findByBranchIdAndProductIdIn(branchId, batch);
            for (InventoryItem item : items) {
                map.put(item.getProductId(), item);
            }
        }
        return map;
    }

    private byte[] buildWorkbook(List<ReportRow> rows, UUID branchId) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");
            int rowIndex = 0;

            BranchClient.BranchInternalDto branch = branchClient.getBranch(branchId);
            String branchLabel = branch == null ? branchId.toString() : branch.name();
            String exportedAt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
                    .format(Instant.now().atZone(ZoneId.systemDefault()));

            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0).setCellValue("Branch");
            titleRow.createCell(1).setCellValue(branchLabel);

            Row timeRow = sheet.createRow(rowIndex++);
            timeRow.createCell(0).setCellValue("Exported at");
            timeRow.createCell(1).setCellValue(exportedAt);

            rowIndex++;

            Row header = sheet.createRow(rowIndex++);
            String[] columns = new String[] { "Product ID", "SKU", "Name", "Category", "Unit", "On Hand",
                    "Reserved", "Available", "Threshold", "Stock Status", "Cost Price", "Sale Price",
                    "Effective Price", "Catalog Status", "Rx Required" };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            for (ReportRow row : rows) {
                Row sheetRow = sheet.createRow(rowIndex++);
                CatalogClient.CatalogProductDto product = row.product;
                int col = 0;
                sheetRow.createCell(col++).setCellValue(asString(product.id()));
                sheetRow.createCell(col++).setCellValue(asString(product.sku()));
                sheetRow.createCell(col++).setCellValue(asString(product.name()));
                sheetRow.createCell(col++).setCellValue(asString(row.categoryName));
                sheetRow.createCell(col++).setCellValue(asString(row.unit));
                sheetRow.createCell(col++).setCellValue(row.onHand);
                sheetRow.createCell(col++).setCellValue(row.reserved);
                sheetRow.createCell(col++).setCellValue(row.available);
                sheetRow.createCell(col++).setCellValue(row.threshold);
                sheetRow.createCell(col++).setCellValue(asString(row.stockStatus));
                sheetRow.createCell(col++).setCellValue(asDouble(product.costPrice()));
                sheetRow.createCell(col++).setCellValue(asDouble(product.baseSalePrice()));
                sheetRow.createCell(col++).setCellValue(asDouble(resolveEffectivePrice(product)));
                sheetRow.createCell(col++).setCellValue(asString(resolveCatalogStatus(product)));
                sheetRow.createCell(col++).setCellValue(product.prescriptionRequired() ? "yes" : "no");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(2, 26 * 256);
            sheet.setColumnWidth(3, 22 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build report");
        }
    }

    private String buildFileName(UUID branchId) {
        BranchClient.BranchInternalDto branch = branchClient.getBranch(branchId);
        String branchToken = branch == null ? branchId.toString().substring(0, 8) : sanitizeFilePart(branch.code());
        String time = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.US)
                .format(Instant.now().atZone(ZoneId.systemDefault()));
        return "inventory-report-" + branchToken + "-" + time + ".xlsx";
    }

    private String sanitizeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "branch";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private BigDecimal resolveEffectivePrice(CatalogClient.CatalogProductDto product) {
        if (product.effectivePrice() != null) {
            return product.effectivePrice();
        }
        return product.baseSalePrice();
    }

    private String resolveCatalogStatus(CatalogClient.CatalogProductDto product) {
        if (product.effectiveStatus() != null && !product.effectiveStatus().isBlank()) {
            return product.effectiveStatus();
        }
        if (product.branchStatus() != null && !product.branchStatus().isBlank()) {
            return product.branchStatus();
        }
        return product.globalStatus();
    }

    private String resolveStockStatus(int onHand, int threshold) {
        if (onHand <= 0) {
            return "out";
        }
        if (onHand <= threshold) {
            return "low";
        }
        return "in";
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.US);
        if (normalized.equals("in") || normalized.equals("low") || normalized.equals("out")) {
            return normalized;
        }
        return null;
    }

    private Attributes parseAttributes(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Attributes(20, "Box");
        }
        try {
            Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
            });
            int threshold = resolveNumber(map.get("threshold"),
                    resolveNumber(map.get("reorderPoint"), resolveNumber(map.get("minStock"), 20)));
            String unit = resolveString(map.get("unit"), "Box");
            return new Attributes(threshold, unit);
        } catch (Exception ex) {
            return new Attributes(20, "Box");
        }
    }

    private int resolveNumber(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String resolveString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private double asDouble(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }

    private record Attributes(int threshold, String unit) {
    }

    private record ReportRow(CatalogClient.CatalogProductDto product, String categoryName, int onHand, int reserved,
            int available, int threshold, String stockStatus, String unit) {
    }

    public record ReportFile(String filename, byte[] content) {
    }
}
