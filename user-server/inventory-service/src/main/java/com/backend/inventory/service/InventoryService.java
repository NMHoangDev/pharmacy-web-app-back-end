package com.backend.inventory.service;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.model.*;
import com.backend.inventory.repo.InventoryActivityRepository;
import com.backend.inventory.repo.InventoryItemRepository;
import com.backend.inventory.repo.ReservationLineRepository;
import com.backend.inventory.repo.ReservationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;
    private final ReservationRepository reservationRepo;
    private final ReservationLineRepository lineRepo;
    private final InventoryActivityRepository activityRepo;
    private final BranchClient branchClient;
    private final UUID defaultBranchId;

    public InventoryService(InventoryItemRepository inventoryRepo,
            ReservationRepository reservationRepo,
            ReservationLineRepository lineRepo,
            InventoryActivityRepository activityRepo,
            BranchClient branchClient,
            @Value("${inventory.default-branch-id}") UUID defaultBranchId) {
        this.inventoryRepo = inventoryRepo;
        this.reservationRepo = reservationRepo;
        this.lineRepo = lineRepo;
        this.activityRepo = activityRepo;
        this.branchClient = branchClient;
        this.defaultBranchId = defaultBranchId;
    }

    public ReserveResponse reserve(ReserveRequest req) {
        if (req.orderId() == null || req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId and items are required");
        }
        UUID branchId = resolveBranchId(req.branchId());
        validateBranchActive(branchId);
        boolean allowNegative = allowNegativeStock(branchId);
        List<UUID> productIds = req.items().stream().map(ItemQuantity::productId).toList();
        List<InventoryItem> items = inventoryRepo.lockAllByBranchIdAndProductIdIn(branchId, productIds);
        Map<UUID, InventoryItem> byId = items.stream().collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        // Ensure records exist for all products
        for (UUID pid : productIds) {
            byId.computeIfAbsent(pid, id -> new InventoryItem(branchId, id, 0, 0));
        }

        // Check availability and update reserved
        for (ItemQuantity iq : req.items()) {
            InventoryItem item = byId.get(iq.productId());
            int available = item.getOnHand() - item.getReserved();
            if (!allowNegative && available < iq.qty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient inventory for product " + iq.productId());
            }
            item.setReserved(item.getReserved() + iq.qty());
            item.setUpdatedAt(Instant.now());
        }
        inventoryRepo.saveAll(byId.values());

        for (ItemQuantity iq : req.items()) {
            InventoryItem item = byId.get(iq.productId());
            logActivity(item.getProductId(), 0, "RESERVE", req.reason(), item.getOnHand(), item.getReserved(),
                    "ORDER", req.orderId(), req.actor(), branchId, null, null);
        }

        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(
                reservationId,
                req.orderId(),
                branchId,
                ReservationStatus.ACTIVE,
                req.ttlSeconds() == null ? null : Instant.now().plusSeconds(req.ttlSeconds()),
                Instant.now());
        reservationRepo.save(reservation);

        List<ReservationLine> lines = req.items().stream()
                .map(i -> new ReservationLine(new ReservationLineId(reservationId, i.productId()), i.qty()))
                .toList();
        lineRepo.saveAll(lines);

        return new ReserveResponse(reservationId, reservation.getStatus().name());
    }

    public ReserveResponse commit(CommitRequest req) {
        UUID branchId = resolveBranchId(req.branchId());
        validateBranchActive(branchId);
        Reservation reservation = findReservation(req.reservationId(), req.orderId(), branchId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation not active");
        }
        List<ReservationLine> lines = lineRepo.findByIdReservationId(reservation.getId());
        applyLines(lines, true, "OUT", req.reason(), req.actor(), branchId,
                resolveRefType(req.orderId()), resolveRefId(req.orderId(), reservation.getId()),
                allowNegativeStock(branchId));
        reservation.setStatus(ReservationStatus.COMMITTED);
        reservationRepo.save(reservation);
        return new ReserveResponse(reservation.getId(), reservation.getStatus().name());
    }

    public ReserveResponse release(ReleaseRequest req) {
        UUID branchId = resolveBranchId(req.branchId());
        validateBranchActive(branchId);
        Reservation reservation = findReservation(req.reservationId(), req.orderId(), branchId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation not active");
        }
        List<ReservationLine> lines = lineRepo.findByIdReservationId(reservation.getId());
        applyLines(lines, false, "RELEASE", req.reason(), req.actor(), branchId,
                resolveRefType(req.orderId()), resolveRefId(req.orderId(), reservation.getId()),
                allowNegativeStock(branchId));
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepo.save(reservation);
        return new ReserveResponse(reservation.getId(), reservation.getStatus().name());
    }

    public AvailabilityResponse availability(UUID branchIdInput, List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new AvailabilityResponse(List.of());
        }
        UUID branchId = resolveBranchId(branchIdInput);
        List<InventoryItem> items = inventoryRepo.findByBranchIdAndProductIdIn(branchId, productIds);
        Map<UUID, InventoryItem> byId = items.stream().collect(Collectors.toMap(InventoryItem::getProductId, i -> i));
        List<AvailabilityItem> result = productIds.stream()
                .map(pid -> {
                    InventoryItem item = byId.get(pid);
                    int onHand = item == null ? 0 : item.getOnHand();
                    int reserved = item == null ? 0 : item.getReserved();
                    int available = Math.max(0, onHand - reserved);
                    return new AvailabilityItem(pid, available, onHand, reserved);
                })
                .toList();
        return new AvailabilityResponse(result);
    }

    public AvailabilityBatchResponse availabilityBatch(AvailabilityBatchRequest request) {
        if (request == null || request.branchIds() == null || request.branchIds().isEmpty()
                || request.items() == null || request.items().isEmpty()) {
            return new AvailabilityBatchResponse(List.of());
        }

        List<UUID> branchIds = request.branchIds();
        List<UUID> productIds = request.items().stream().map(ItemQuantity::productId).toList();
        List<InventoryItem> items = inventoryRepo.findByBranchIdInAndProductIdIn(branchIds, productIds);
        Map<UUID, Map<UUID, InventoryItem>> byBranch = new HashMap<>();
        for (InventoryItem item : items) {
            byBranch
                    .computeIfAbsent(item.getBranchId(), key -> new HashMap<>())
                    .put(item.getProductId(), item);
        }

        List<AvailabilityBatchItem> result = productIds.stream().map(pid -> {
            List<AvailabilityByBranch> byBranchResult = branchIds.stream().map(branchId -> {
                InventoryItem item = byBranch.getOrDefault(branchId, Map.of()).get(pid);
                int onHand = item == null ? 0 : item.getOnHand();
                int reserved = item == null ? 0 : item.getReserved();
                int available = Math.max(0, onHand - reserved);
                return new AvailabilityByBranch(branchId, available, onHand, reserved);
            }).toList();
            return new AvailabilityBatchItem(pid, byBranchResult);
        }).toList();

        return new AvailabilityBatchResponse(result);
    }

    public AdjustResponse adjust(AdjustRequest req) {
        if (req.productId() == null || req.delta() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and delta are required");
        }
        UUID branchId = resolveBranchId(req.branchId());
        validateBranchActive(branchId);
        boolean allowNegative = allowNegativeStock(branchId);
        InventoryItemId itemId = new InventoryItemId(branchId, req.productId());
        InventoryItem item = inventoryRepo.findById(itemId)
                .orElseGet(() -> new InventoryItem(branchId, req.productId(), 0, 0));
        int newOnHand = item.getOnHand() + req.delta();
        if (!allowNegative && newOnHand < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "On hand cannot be negative");
        }
        item.setOnHand(newOnHand);
        item.setUpdatedAt(Instant.now());
        inventoryRepo.save(item);
        logActivity(item.getProductId(), req.delta(), resolveActivityType(req.delta(), req.reason()),
                req.reason(), item.getOnHand(), item.getReserved(), req.refType(), req.refId(), req.actor(),
                branchId, req.batchNo(), req.expiryDate());
        return new AdjustResponse(item.getProductId(), item.getOnHand(), item.getReserved());
    }

    public boolean deleteItem(UUID productId, UUID branchIdInput) {
        if (productId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }
        UUID branchId = resolveBranchId(branchIdInput);
        InventoryItemId id = new InventoryItemId(branchId, productId);
        InventoryItem item = inventoryRepo.findById(id).orElse(null);
        if (item == null) {
            return false;
        }
        inventoryRepo.deleteById(id);
        logActivity(productId, 0, "DELETE", "Removed from inventory", item.getOnHand(), item.getReserved(),
                null, null, null, branchId, null, null);
        return true;
    }

    public List<InventoryActivityResponse> listActivities(UUID productId, UUID branchIdInput, int limit) {
        int safeLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 200));
        Pageable pageable = PageRequest.of(0, safeLimit);
        UUID branchId = resolveBranchId(branchIdInput);
        List<InventoryActivity> activities = productId == null
                ? activityRepo.findRecentByBranchId(branchId, pageable)
                : activityRepo.findRecentByBranchIdAndProductId(branchId, productId, pageable);
        return activities.stream()
                .map(a -> new InventoryActivityResponse(a.getId(), a.getProductId(), a.getType(), a.getDelta(),
                        a.getOnHandAfter(), a.getReservedAfter(), a.getReason(), a.getRefType(), a.getRefId(),
                        a.getActor(), a.getBranchId(), a.getBatchNo(), a.getExpiryDate(), a.getCreatedAt()))
                .toList();
    }

    private void logActivity(UUID productId, int delta, String type, String reason, int onHand, int reserved,
            String refType, UUID refId, String actor, UUID branchId, String batchNo, LocalDate expiryDate) {
        InventoryActivity activity = new InventoryActivity();
        activity.setId(UUID.randomUUID());
        activity.setProductId(productId);
        activity.setType(type);
        activity.setDelta(delta);
        activity.setOnHandAfter(onHand);
        activity.setReservedAfter(reserved);
        activity.setReason(reason);
        activity.setRefType(refType);
        activity.setRefId(refId);
        activity.setActor(actor);
        activity.setBranchId(branchId);
        activity.setBatchNo(batchNo);
        activity.setExpiryDate(expiryDate);
        activity.setCreatedAt(Instant.now());
        activityRepo.save(activity);
    }

    private String resolveActivityType(int delta, String reason) {
        if (reason != null && reason.toLowerCase().contains("inventory check")) {
            return "ADJUST";
        }
        if (delta > 0) {
            return "IN";
        }
        if (delta < 0) {
            return "OUT";
        }
        return "ADJUST";
    }

    private void applyLines(List<ReservationLine> lines, boolean commit, String activityType, String reason,
            String actor, UUID branchId, String refType, UUID refId, boolean allowNegative) {
        if (lines.isEmpty()) {
            return;
        }
        List<UUID> productIds = lines.stream().map(l -> l.getId().getProductId()).toList();
        List<InventoryItem> items = inventoryRepo.lockAllByBranchIdAndProductIdIn(branchId, productIds);
        Map<UUID, InventoryItem> byId = items.stream().collect(Collectors.toMap(InventoryItem::getProductId, i -> i));
        for (ReservationLine line : lines) {
            UUID pid = line.getId().getProductId();
            InventoryItem item = byId.get(pid);
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory missing for product " + pid);
            }
            int qty = line.getQuantity();
            if (item.getReserved() < qty) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved less than required for product " + pid);
            }
            item.setReserved(item.getReserved() - qty);
            if (commit) {
                if (!allowNegative && item.getOnHand() < qty) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "On hand insufficient for product " + pid);
                }
                item.setOnHand(item.getOnHand() - qty);
            }
            item.setUpdatedAt(Instant.now());
            int delta = commit ? -qty : 0;
            logActivity(pid, delta, activityType, reason, item.getOnHand(), item.getReserved(), refType, refId,
                    actor, branchId, null, null);
        }
        inventoryRepo.saveAll(byId.values());
    }

    private String resolveRefType(UUID orderId) {
        return orderId == null ? "RESERVATION" : "ORDER";
    }

    private UUID resolveRefId(UUID orderId, UUID reservationId) {
        return orderId == null ? reservationId : orderId;
    }

    private Reservation findReservation(UUID reservationId, UUID orderId, UUID branchId) {
        if (reservationId != null) {
            return reservationRepo.findById(reservationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        }
        if (orderId != null) {
            return reservationRepo.findByOrderIdAndBranchId(orderId, branchId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reservationId or orderId is required");
    }

    @Scheduled(fixedDelayString = "${inventory.reservation.expireDelayMs:60000}")
    public void expireReservations() {
        List<Reservation> expired = reservationRepo.findByStatusAndExpiresAtBefore(
                ReservationStatus.ACTIVE, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (Reservation reservation : expired) {
            List<ReservationLine> lines = lineRepo.findByIdReservationId(reservation.getId());
            UUID branchId = resolveBranchId(reservation.getBranchId());
            applyLines(lines, false, "EXPIRE", "Reservation expired", null, branchId,
                    "RESERVATION", reservation.getId(), allowNegativeStock(branchId));
            reservation.setStatus(ReservationStatus.EXPIRED);
        }
        reservationRepo.saveAll(expired);
    }

    private UUID resolveBranchId(UUID branchId) {
        return branchId == null ? defaultBranchId : branchId;
    }

    private void validateBranchActive(UUID branchId) {
        BranchClient.BranchInternalDto branch = branchClient.getBranch(branchId);
        if (branch == null) {
            if (Objects.equals(branchId, defaultBranchId)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Branch service unavailable");
        }
        if (branch.status() == null || !branch.status().equalsIgnoreCase("ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is not active");
        }
    }

    private boolean allowNegativeStock(UUID branchId) {
        BranchClient.BranchSettingsDto settings = branchClient.getSettings(branchId);
        if (settings == null) {
            return false;
        }
        return Boolean.TRUE.equals(settings.allowNegativeStock());
    }
}
