package com.backend.pharmacist.api;

import com.backend.pharmacist.api.dto.InternalPharmacistBootstrapRequest;
import com.backend.pharmacist.api.dto.PharmacistResponse;
import com.backend.pharmacist.service.PharmacistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/pharmacists")
public class InternalPharmacistSyncController {

    private final PharmacistService pharmacistService;

    public InternalPharmacistSyncController(PharmacistService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    @PutMapping("/{id}/bootstrap")
    public ResponseEntity<PharmacistResponse> bootstrap(
            @PathVariable("id") UUID id,
            @RequestBody(required = false) InternalPharmacistBootstrapRequest request) {
        return ResponseEntity.ok(pharmacistService.bootstrapProfile(id, request));
    }
}
