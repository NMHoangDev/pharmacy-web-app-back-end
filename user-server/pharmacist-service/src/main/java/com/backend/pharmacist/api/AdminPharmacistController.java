package com.backend.pharmacist.api;

import com.backend.pharmacist.api.dto.*;
import com.backend.pharmacist.service.PharmacistService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pharmacists")
public class AdminPharmacistController {

    private final PharmacistService pharmacistService;

    public AdminPharmacistController(PharmacistService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    @GetMapping
    public ResponseEntity<Page<PharmacistResponse>> list(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "specialty", required = false) String specialty,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "experience", required = false) String experience,
            @RequestParam(name = "verification", required = false) String verification,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Boolean verified = null;
        if ("verified".equalsIgnoreCase(verification)) {
            verified = true;
        } else if ("pending".equalsIgnoreCase(verification)) {
            verified = false;
        }
        return ResponseEntity
                .ok(pharmacistService.list(query, specialty, status, mode, experience, verified, page, size));
    }

    @PostMapping
    public ResponseEntity<PharmacistResponse> create(@RequestBody @Valid UpsertPharmacistRequest request) {
        return ResponseEntity.ok(pharmacistService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacistResponse> detail(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(pharmacistService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PharmacistResponse> update(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpsertPharmacistRequest request) {
        return ResponseEntity.ok(pharmacistService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        pharmacistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<PharmacistResponse> verify(
            @PathVariable("id") UUID id,
            @RequestBody @Valid VerifyPharmacistRequest request) {
        return ResponseEntity.ok(pharmacistService.updateVerification(id, request.verified()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PharmacistResponse> updateStatus(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateStatusRequest request) {
        return ResponseEntity.ok(pharmacistService.updateStatus(id, request));
    }

    @PostMapping("/{id}/shifts")
    public ResponseEntity<ShiftResponse> createShift(
            @PathVariable("id") UUID id,
            @RequestBody @Valid ShiftRequest request) {
        return ResponseEntity.ok(pharmacistService.createShift(id, request));
    }

    @GetMapping("/{id}/shifts")
    public ResponseEntity<List<ShiftResponse>> listShifts(
            @PathVariable("id") UUID id,
            @RequestParam(name = "from", required = false) LocalDateTime from,
            @RequestParam(name = "to", required = false) LocalDateTime to) {
        return ResponseEntity.ok(pharmacistService.listShifts(id, from, to));
    }
}
