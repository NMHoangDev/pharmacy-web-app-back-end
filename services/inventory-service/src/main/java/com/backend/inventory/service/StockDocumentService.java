package com.backend.inventory.service;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.model.*;
import com.backend.inventory.repo.InventoryActivityRepository;
import com.backend.inventory.repo.InventoryItemRepository;
import com.backend.inventory.repo.StockDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockDocumentService {

    private final StockDocumentRepository stockDocumentRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryActivityRepository activityRepository;
    private final BranchClient branchClient;

    public StockDocumentService(StockDocumentRepository stockDocumentRepository,
            InventoryItemRepository inventoryItemRepository,
            InventoryActivityRepository activityRepository,
            BranchClient branchClient) {
        this.stockDocumentRepository = stockDocumentRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.activityRepository = activityRepository;
        this.branchClient = branchClient;
    }

    public StockDocumentResponse createDraft(StockDocumentCreateRequest request) {
        validateCreateRequest(request);
        validateBranchActive(request.branchId());

        StockDocumentEntity document = new StockDocumentEntity();
        document.setId(UUID.randomUUID());
        document.setType(request.type());
        document.setStatus(StockDocumentStatus.DRAFT);
        document.setSupplierName(request.supplierName());
        document.setSupplierId(request.supplierId());
        document.setInvoiceNo(request.invoiceNo());
        document.setReason(request.reason());
        document.setCreatedBy(request.createdBy());
        document.setBranchId(request.branchId());
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());

        for (StockDocumentLineRequest line : request.lines()) {
            StockDocumentLineEntity entity = new StockDocumentLineEntity();
            entity.setId(UUID.randomUUID());
            entity.setProductId(line.productId());
            entity.setSkuSnapshot(line.skuSnapshot());
            entity.setQuantity(line.quantity());
            entity.setUnitCost(line.unitCost());
            entity.setBatchNo(line.batchNo());
            entity.setExpiryDate(line.expiryDate());
            document.addLine(entity);
        }

        StockDocumentEntity saved = stockDocumentRepository.save(document);
        return toResponse(saved);
    }

    public StockDocumentResponse submit(UUID id, StockDocumentSubmitRequest request) {
        StockDocumentEntity document = getById(id);
        if (document.getStatus() != StockDocumentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft documents can be submitted");
        }
        document.setStatus(StockDocumentStatus.SUBMITTED);
        document.setSubmittedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        if (document.getCreatedBy() == null && request != null) {
            document.setCreatedBy(request.actor());
        }
        return toResponse(stockDocumentRepository.save(document));
    }

    public StockDocumentResponse approve(UUID id, StockDocumentApproveRequest request) {
        StockDocumentEntity document = getWithLines(id);
        if (document.getStatus() != StockDocumentStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted documents can be approved");
        }
        validateBranchActive(document.getBranchId());
        if (document.getLines() == null || document.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must have at least one line");
        }
        applyStockDocument(document, request == null ? null : request.actor());
        document.setStatus(StockDocumentStatus.APPROVED);
        document.setApprovedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        if (request != null && request.actor() != null) {
            document.setApprovedBy(request.actor());
        }
        if (document.getApprovedBy() == null) {
            document.setApprovedBy(document.getCreatedBy());
        }
        return toResponse(stockDocumentRepository.save(document));
    }

    public StockDocumentResponse reject(UUID id, StockDocumentRejectRequest request) {
        StockDocumentEntity document = getById(id);
        if (document.getStatus() != StockDocumentStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only submitted documents can be rejected");
        }
        document.setStatus(StockDocumentStatus.REJECTED);
        document.setUpdatedAt(Instant.now());
        document.setApprovedAt(Instant.now());
        if (request != null && request.actor() != null) {
            document.setApprovedBy(request.actor());
        }
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            String existing = document.getReason();
            document.setReason(existing == null || existing.isBlank()
                    ? request.reason()
                    : existing + " | Rejected: " + request.reason());
        }
        return toResponse(stockDocumentRepository.save(document));
    }

    public StockDocumentResponse get(UUID id) {
        return toResponse(getWithLines(id));
    }

    public StockDocumentPageResponse list(StockDocumentType type,
            StockDocumentStatus status,
            UUID branchId,
            Instant fromDate,
            Instant toDate,
            String keyword,
            int page,
            int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size <= 0 ? 20 : size, 200));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<StockDocumentEntity> result = stockDocumentRepository.search(type, status, branchId, fromDate, toDate,
                normalizeKeyword(keyword), pageable);
        List<StockDocumentSummaryResponse> items = result.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new StockDocumentPageResponse(items, safePage, safeSize, result.getTotalElements());
    }

    private void validateCreateRequest(StockDocumentCreateRequest request) {
        if (request == null || request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        if (request.branchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lines are required");
        }
        for (StockDocumentLineRequest line : request.lines()) {
            if (line.productId() == null || line.quantity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and quantity are required");
            }
            if (request.type() == StockDocumentType.ADJUST) {
                if (line.quantity() == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity cannot be 0 for ADJUST");
                }
            } else if (line.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be positive");
            }
        }
    }

    private void applyStockDocument(StockDocumentEntity document, String actor) {
        List<UUID> productIds = document.getLines().stream()
                .map(StockDocumentLineEntity::getProductId)
                .distinct()
                .toList();
        List<InventoryItem> locked = inventoryItemRepository.lockAllByBranchIdAndProductIdIn(document.getBranchId(),
                productIds);
        Map<UUID, InventoryItem> byId = locked.stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        for (StockDocumentLineEntity line : document.getLines()) {
            InventoryItem item = byId.get(line.getProductId());
            int delta = resolveDelta(document.getType(), line.getQuantity());
            if (item == null) {
                if (delta < 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Inventory missing for product " + line.getProductId());
                }
                item = new InventoryItem(document.getBranchId(), line.getProductId(), 0, 0);
                byId.put(line.getProductId(), item);
            }
            int newOnHand = item.getOnHand() + delta;
            if (newOnHand < 0 && !allowNegativeStock(document.getBranchId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "On hand cannot be negative for product " + line.getProductId());
            }
            item.setOnHand(newOnHand);
            item.setUpdatedAt(Instant.now());

            logActivity(item, delta, document, actor, line);
        }

        inventoryItemRepository.saveAll(byId.values());
    }

    private void validateBranchActive(UUID branchId) {
        BranchClient.BranchInternalDto branch = branchClient.getBranch(branchId);
        if (branch == null || branch.status() == null || !branch.status().equalsIgnoreCase("ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is not active");
        }
    }

    private boolean allowNegativeStock(UUID branchId) {
        BranchClient.BranchSettingsDto settings = branchClient.getSettings(branchId);
        return settings != null && Boolean.TRUE.equals(settings.allowNegativeStock());
    }

    private int resolveDelta(StockDocumentType type, int quantity) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
        }
        if (type == StockDocumentType.OUT) {
            return -quantity;
        }
        // IN and ADJUST apply as-is.
        return quantity;
    }

    private void logActivity(InventoryItem item, int delta, StockDocumentEntity document, String actor,
            StockDocumentLineEntity line) {
        InventoryActivity activity = new InventoryActivity();
        activity.setId(UUID.randomUUID());
        activity.setProductId(item.getProductId());
        activity.setType(document.getType().name());
        activity.setDelta(delta);
        activity.setOnHandAfter(item.getOnHand());
        activity.setReservedAfter(item.getReserved());
        activity.setReason(document.getReason());
        activity.setRefType("STOCK_DOCUMENT");
        activity.setRefId(document.getId());
        activity.setActor(actor != null ? actor : document.getApprovedBy());
        activity.setBranchId(document.getBranchId());
        activity.setBatchNo(line.getBatchNo());
        activity.setExpiryDate(line.getExpiryDate());
        activity.setCreatedAt(Instant.now());
        activityRepository.save(activity);
    }

    private StockDocumentEntity getById(UUID id) {
        return stockDocumentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private StockDocumentEntity getWithLines(UUID id) {
        return stockDocumentRepository.findWithLinesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private StockDocumentResponse toResponse(StockDocumentEntity document) {
        List<StockDocumentLineResponse> lines = document.getLines() == null ? List.of()
                : document.getLines().stream()
                        .map(this::toLineResponse)
                        .toList();
        return new StockDocumentResponse(
                document.getId(),
                document.getType(),
                document.getStatus(),
                document.getSupplierName(),
                document.getSupplierId(),
                document.getInvoiceNo(),
                document.getReason(),
                document.getCreatedBy(),
                document.getApprovedBy(),
                document.getBranchId(),
                document.getCreatedAt(),
                document.getSubmittedAt(),
                document.getApprovedAt(),
                document.getUpdatedAt(),
                lines);
    }

    private StockDocumentLineResponse toLineResponse(StockDocumentLineEntity line) {
        return new StockDocumentLineResponse(
                line.getId(),
                line.getProductId(),
                line.getSkuSnapshot(),
                line.getQuantity(),
                line.getUnitCost(),
                line.getBatchNo(),
                line.getExpiryDate());
    }

    private StockDocumentSummaryResponse toSummary(StockDocumentEntity document) {
        int count = document.getLines() == null ? 0 : document.getLines().size();
        return new StockDocumentSummaryResponse(
                document.getId(),
                document.getType(),
                document.getStatus(),
                document.getSupplierName(),
                document.getInvoiceNo(),
                document.getCreatedBy(),
                document.getApprovedBy(),
                document.getBranchId(),
                document.getCreatedAt(),
                document.getSubmittedAt(),
                document.getApprovedAt(),
                document.getUpdatedAt(),
                count);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
