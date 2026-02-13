package com.backend.pharmacist.api;

import com.backend.pharmacist.api.dto.PharmacistResponse;
import com.backend.pharmacist.service.PharmacistService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacists")
public class PublicPharmacistController {

    private final PharmacistService pharmacistService;

    public PublicPharmacistController(PharmacistService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    @GetMapping
    public ResponseEntity<Page<PharmacistResponse>> list(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "specialty", required = false) String specialty,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "mode", required = false) String mode,
            @RequestParam(name = "experience", required = false) String experience,
            @RequestParam(name = "verified", required = false) Boolean verified,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity
                .ok(pharmacistService.list(query, specialty, status, mode, experience, verified, page, size,
                        branchId));
    }

    @GetMapping("/online")
    public ResponseEntity<List<PharmacistResponse>> online(
            @RequestParam(name = "limit", defaultValue = "6") int limit,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return ResponseEntity.ok(pharmacistService.listOnline(limit, branchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacistResponse> detail(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(pharmacistService.get(id));
    }
}
