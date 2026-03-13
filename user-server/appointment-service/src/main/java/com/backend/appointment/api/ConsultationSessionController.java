package com.backend.appointment.api;

import com.backend.appointment.api.dto.ConsultationPrescriptionOrderRequest;
import com.backend.appointment.api.dto.ConsultationPrescriptionOrderResponse;
import com.backend.appointment.api.dto.ConsultationPrescriptionProductPageResponse;
import com.backend.appointment.api.dto.ConsultationRequest;
import com.backend.appointment.api.dto.ConsultationResponse;
import com.backend.appointment.service.ConsultationService;
import com.backend.appointment.service.CurrentPharmacistResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ConsultationSessionController {

    private final ConsultationService consultationService;
    private final CurrentPharmacistResolver pharmacistResolver;

    public ConsultationSessionController(ConsultationService consultationService,
            CurrentPharmacistResolver pharmacistResolver) {
        this.consultationService = consultationService;
        this.pharmacistResolver = pharmacistResolver;
    }

    @PostMapping("/appointments/{appointmentId}/session")
    public ResponseEntity<ConsultationResponse> createOrGetSession(
            @PathVariable String appointmentId,
            @Valid @RequestBody ConsultationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        // userId = jwt.getSubject()
        return ResponseEntity.ok(consultationService.createSession(appointmentId, jwt.getSubject(), request));
    }

    @PostMapping("/consultations/{roomId}/join")
    public ResponseEntity<ConsultationResponse> joinSession(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(consultationService.joinSession(roomId, jwt.getSubject()));
    }

    @PostMapping("/consultations/{roomId}/leave")
    public ResponseEntity<ConsultationResponse> leaveSession(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(consultationService.leaveSession(roomId, jwt.getSubject()));
    }

    @GetMapping("/consultations/{roomId}")
    public ResponseEntity<ConsultationResponse> getSessionDetails(
            @PathVariable String roomId,
            @AuthenticationPrincipal Jwt jwt) {
        // Implement getSessionDetails in service if missing, using existing
        // findByRoomId
        return ResponseEntity.ok(consultationService.joinSession(roomId, jwt.getSubject())); // joinSession also returns
                                                                                             // details
    }

    @PutMapping("/consultations/{appointmentId}/notes")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<Void> updateNotes(
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal Jwt jwt) {
        consultationService.updateNotes(appointmentId, resolvePharmacistId().toString(), payload.get("notes"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/consultations/{appointmentId}/prescription/products")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<ConsultationPrescriptionProductPageResponse> searchPrescriptionProducts(
            @PathVariable String appointmentId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .ok(consultationService.searchPrescriptionProducts(
                        appointmentId,
                        resolvePharmacistId().toString(),
                        query,
                        page,
                        size));
    }

    @PostMapping("/consultations/{appointmentId}/prescription/orders")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<ConsultationPrescriptionOrderResponse> createPrescriptionOrder(
            @PathVariable String appointmentId,
            @Valid @RequestBody ConsultationPrescriptionOrderRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity
                .ok(consultationService.createPrescriptionOrder(
                        appointmentId,
                        resolvePharmacistId().toString(),
                        request));
    }

    private UUID resolvePharmacistId() {
        UUID pharmacistId = pharmacistResolver.resolveForCurrentActor();
        if (pharmacistId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unable to resolve pharmacist identity");
        }
        return pharmacistId;
    }
}
