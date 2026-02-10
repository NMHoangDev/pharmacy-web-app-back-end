package com.backend.appointment.api;

import com.backend.appointment.api.dto.ConsultationRequest;
import com.backend.appointment.api.dto.ConsultationResponse;
import com.backend.appointment.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConsultationSessionController {

    private final ConsultationService consultationService;

    public ConsultationSessionController(ConsultationService consultationService) {
        this.consultationService = consultationService;
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
        consultationService.updateNotes(appointmentId, jwt.getSubject(), payload.get("notes"));
        return ResponseEntity.ok().build();
    }
}
