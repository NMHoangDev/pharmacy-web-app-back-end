package com.backend.inventory.service;

import com.backend.inventory.api.dto.*;
import com.backend.inventory.model.*;
import com.backend.inventory.repo.InventoryItemRepository;
import com.backend.inventory.repo.ReservationLineRepository;
import com.backend.inventory.repo.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    private final InventoryItemRepository inventoryRepo;
    private final ReservationRepository reservationRepo;
    private final ReservationLineRepository lineRepo;

    public InventoryService(InventoryItemRepository inventoryRepo,
            ReservationRepository reservationRepo,
            ReservationLineRepository lineRepo) {
        this.inventoryRepo = inventoryRepo;
        this.reservationRepo = reservationRepo;
        this.lineRepo = lineRepo;
    }

    public ReserveResponse reserve(ReserveRequest req) {
        if (req.orderId() == null || req.items() == null || req.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId and items are required");
        }
        List<UUID> productIds = req.items().stream().map(ItemQuantity::productId).toList();
        List<InventoryItem> items = inventoryRepo.lockAllByProductIdIn(productIds);
        Map<UUID, InventoryItem> byId = items.stream().collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        // Ensure records exist for all products
        for (UUID pid : productIds) {
            byId.computeIfAbsent(pid, id -> new InventoryItem(id, 0, 0));
        }

        // Check availability and update reserved
        for (ItemQuantity iq : req.items()) {
            InventoryItem item = byId.get(iq.productId());
            int available = item.getOnHand() - item.getReserved();
            if (available < iq.qty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient inventory for product " + iq.productId());
            }
            item.setReserved(item.getReserved() + iq.qty());
            item.setUpdatedAt(Instant.now());
        }
        inventoryRepo.saveAll(byId.values());

        UUID reservationId = UUID.randomUUID();
        Reservation reservation = new Reservation(
                reservationId,
                req.orderId(),
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
        Reservation reservation = findReservation(req.reservationId(), req.orderId());
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation not active");
        }
        List<ReservationLine> lines = lineRepo.findByIdReservationId(reservation.getId());
        applyLines(lines, true);
        reservation.setStatus(ReservationStatus.COMMITTED);
        reservationRepo.save(reservation);
        return new ReserveResponse(reservation.getId(), reservation.getStatus().name());
    }

    public ReserveResponse release(ReleaseRequest req) {
        Reservation reservation = findReservation(req.reservationId(), req.orderId());
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservation not active");
        }
        List<ReservationLine> lines = lineRepo.findByIdReservationId(reservation.getId());
        applyLines(lines, false);
        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepo.save(reservation);
        return new ReserveResponse(reservation.getId(), reservation.getStatus().name());
    }

    public AvailabilityResponse availability(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new AvailabilityResponse(List.of());
        }
        List<InventoryItem> items = inventoryRepo.findByProductIdIn(productIds);
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

    public AdjustResponse adjust(AdjustRequest req) {
        if (req.productId() == null || req.delta() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and delta are required");
        }
        InventoryItem item = inventoryRepo.findById(req.productId())
                .orElseGet(() -> new InventoryItem(req.productId(), 0, 0));
        int newOnHand = item.getOnHand() + req.delta();
        if (newOnHand < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "On hand cannot be negative");
        }
        item.setOnHand(newOnHand);
        item.setUpdatedAt(Instant.now());
        inventoryRepo.save(item);
        return new AdjustResponse(item.getProductId(), item.getOnHand(), item.getReserved());
    }

    private void applyLines(List<ReservationLine> lines, boolean commit) {
        if (lines.isEmpty()) {
            return;
        }
        List<UUID> productIds = lines.stream().map(l -> l.getId().getProductId()).toList();
        List<InventoryItem> items = inventoryRepo.lockAllByProductIdIn(productIds);
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
                if (item.getOnHand() < qty) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "On hand insufficient for product " + pid);
                }
                item.setOnHand(item.getOnHand() - qty);
            }
            item.setUpdatedAt(Instant.now());
        }
        inventoryRepo.saveAll(byId.values());
    }

    private Reservation findReservation(UUID reservationId, UUID orderId) {
        if (reservationId != null) {
            return reservationRepo.findById(reservationId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        }
        if (orderId != null) {
            return reservationRepo.findByOrderId(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reservationId or orderId is required");
    }
}
