package com.backend.appointment.api;

import com.backend.appointment.api.dto.AppointmentRequest;
import com.backend.appointment.api.dto.AppointmentResponse;
import com.backend.appointment.api.dto.CancelRequest;
import com.backend.appointment.api.dto.RescheduleRequest;
import com.backend.appointment.service.AppointmentService;
import com.backend.appointment.service.AppointmentAccessService;
import com.backend.appointment.service.CurrentPharmacistResolver;
import com.backend.appointment.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;
    private final AppointmentAccessService accessService;
    private final CurrentPharmacistResolver pharmacistResolver;

    public AppointmentController(AppointmentService service, AppointmentAccessService accessService,
            CurrentPharmacistResolver pharmacistResolver) {
        this.service = service;
        this.accessService = accessService;
        this.pharmacistResolver = pharmacistResolver;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("appointment-service ok");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<AppointmentResponse> create(@RequestBody @Valid AppointmentRequest req,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.create(req, extractIp(request)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<AppointmentResponse>> byUser(@PathVariable UUID userId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!SecurityUtils.isAdmin()) {
            UUID actorId = SecurityUtils.getActorId();
            if (actorId == null || !actorId.equals(userId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Not allowed");
            }
        }
        return ResponseEntity.ok(service.listByUser(userId, branchId, page, size));
    }

    @GetMapping("/pharmacist/{pharmacistId}")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<Page<AppointmentResponse>> byPharmacist(@PathVariable UUID pharmacistId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!SecurityUtils.isAdmin() && !pharmacistResolver.canAccessPharmacistId(pharmacistId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Not allowed");
        }
        return ResponseEntity.ok(service.listByPharmacist(pharmacistId, branchId, page, size));
    }

    @GetMapping("/pharmacist/me")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<Page<AppointmentResponse>> myAppointments(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID pharmacistId = pharmacistResolver.resolveForCurrentActor();
        if (pharmacistId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Unable to resolve pharmacist identity");
        }
        return ResponseEntity.ok(service.listByPharmacist(pharmacistId, branchId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','USER','ADMIN')")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        boolean includeNotes = SecurityUtils.isPharmacist();
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isPharmacist()) {
            accessService.requireParticipant(id, SecurityUtils.getActorId());
        }
        return ResponseEntity.ok(service.get(id, includeNotes));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id, HttpServletRequest request) {
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        }
        return ResponseEntity.ok(service.confirm(id, null, extractIp(request)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER','PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id,
            @RequestBody(required = false) CancelRequest body,
            HttpServletRequest request) {
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        } else if (!SecurityUtils.isAdmin()) {
            accessService.requireParticipant(id, SecurityUtils.getActorId());
        }
        return ResponseEntity.ok(service.cancel(id, body != null ? body.reason() : null, extractIp(request)));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<AppointmentResponse> reschedule(@PathVariable UUID id,
            @RequestBody @Valid RescheduleRequest body,
            HttpServletRequest request) {
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        }
        return ResponseEntity.ok(service.reschedule(id, body.startAt(), body.endAt(), body.reason(),
                extractIp(request)));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<AppointmentResponse> start(@PathVariable UUID id, HttpServletRequest request) {
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        }
        return ResponseEntity.ok(service.start(id, extractIp(request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id, HttpServletRequest request) {
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        }
        return ResponseEntity.ok(service.complete(id, extractIp(request)));
    }

    @GetMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('PHARMACIST','STAFF','ADMIN')")
    public ResponseEntity<String> getNotes(@PathVariable UUID id,
            @RequestParam(required = false) String reason,
            HttpServletRequest request) {
        if (SecurityUtils.isAdmin() && (reason == null || reason.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "reason required");
        }
        if (SecurityUtils.isPharmacist()) {
            accessService.requirePharmacist(id, pharmacistResolver.resolveForCurrentActor());
        }
        // log break-glass for admin
        if (SecurityUtils.isAdmin()) {
            service.logBreakGlass(id, reason, extractIp(request));
        }
        return ResponseEntity.ok(service.get(id, true).notes());
    }

    private String extractIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
