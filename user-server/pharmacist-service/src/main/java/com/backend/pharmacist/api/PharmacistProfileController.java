package com.backend.pharmacist.api;

import com.backend.pharmacist.api.dto.PharmacistResponse;
import com.backend.pharmacist.api.dto.UpsertPharmacistRequest;
import com.backend.pharmacist.security.SecurityUtils;
import com.backend.pharmacist.service.PharmacistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pharmacists/me")
@PreAuthorize("hasAnyRole('PHARMACIST','ADMIN')")
public class PharmacistProfileController {

    private final PharmacistService pharmacistService;

    public PharmacistProfileController(PharmacistService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    @GetMapping
    public ResponseEntity<PharmacistResponse> getMyProfile() {
        UUID actorId = SecurityUtils.getActorId();
        return ResponseEntity.ok(pharmacistService.getOwnProfile(actorId));
    }

    @PutMapping
    public ResponseEntity<PharmacistResponse> updateMyProfile(@RequestBody @Valid UpsertPharmacistRequest request) {
        UUID actorId = SecurityUtils.getActorId();
        return ResponseEntity.ok(pharmacistService.upsertOwnProfile(actorId, request));
    }
}
