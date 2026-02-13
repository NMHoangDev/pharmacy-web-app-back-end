package com.backend.appointment.api;

import com.backend.appointment.api.dto.AssignRequest;
import com.backend.appointment.api.dto.RescheduleRequest;
import com.backend.appointment.api.dto.StatusUpdateRequest;
import com.backend.appointment.api.dto.TimeOffRequest;
import com.backend.appointment.api.dto.RosterRequest;
import com.backend.appointment.api.dto.AppointmentResponse;
import com.backend.appointment.model.AppointmentAuditLog;
import com.backend.appointment.model.PharmacistRoster;
import com.backend.appointment.model.PharmacistTimeOff;
import com.backend.appointment.repo.AppointmentAuditLogRepository;
import com.backend.appointment.repo.PharmacistRosterRepository;
import com.backend.appointment.repo.PharmacistTimeOffRepository;
import com.backend.appointment.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {

    private final AppointmentService service;
    private final PharmacistRosterRepository rosterRepository;
    private final PharmacistTimeOffRepository timeOffRepository;
    private final AppointmentAuditLogRepository auditLogRepository;

    public AdminAppointmentController(AppointmentService service,
            PharmacistRosterRepository rosterRepository,
            PharmacistTimeOffRepository timeOffRepository,
            AppointmentAuditLogRepository auditLogRepository) {
        this.service = service;
        this.rosterRepository = rosterRepository;
        this.timeOffRepository = timeOffRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable UUID id,
            @RequestBody @Valid StatusUpdateRequest body,
            HttpServletRequest request) {
        String status = body.status() != null ? body.status().toUpperCase() : "";
        return ResponseEntity.ok(service.updateStatusByAdmin(id, status, body.reason(), extractIp(request)));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> listByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "branchId", required = false) UUID branchId) {
        return ResponseEntity.ok(service.listByDate(date, branchId));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<AppointmentResponse> assign(@PathVariable UUID id,
            @RequestBody @Valid AssignRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.assign(id, body.pharmacistId(), body.reason(), extractIp(request)));
    }

    @PostMapping("/{id}/auto-assign")
    public ResponseEntity<AppointmentResponse> autoAssign(@PathVariable UUID id,
            @RequestParam(name = "strategy", defaultValue = "least_loaded") String strategy,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.autoAssign(id, strategy, extractIp(request)));
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(@PathVariable UUID id,
            @RequestBody @Valid RescheduleRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.reschedule(id, body.startAt(), body.endAt(), body.reason(),
                extractIp(request)));
    }

    @PostMapping("/{id}/no-show")
    public ResponseEntity<AppointmentResponse> markNoShow(@PathVariable UUID id,
            @RequestBody(required = false) StatusUpdateRequest body,
            HttpServletRequest request) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(service.noShow(id, reason, extractIp(request)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<AppointmentResponse> refund(@PathVariable UUID id,
            @RequestBody(required = false) StatusUpdateRequest body,
            HttpServletRequest request) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(service.refund(id, reason, extractIp(request)));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AppointmentAuditLog>> auditLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogRepository.findByAppointmentIdOrderByCreatedAtDesc(id));
    }

    @PostMapping("/roster")
    public ResponseEntity<PharmacistRoster> upsertRoster(@RequestBody @Valid RosterRequest body) {
        PharmacistRoster roster = new PharmacistRoster();
        roster.setId(UUID.randomUUID());
        roster.setPharmacistId(body.pharmacistId());
        roster.setBranchId(body.branchId());
        roster.setDayOfWeek(body.dayOfWeek());
        roster.setStartTime(body.startTime());
        roster.setEndTime(body.endTime());
        return ResponseEntity.ok(rosterRepository.save(roster));
    }

    @DeleteMapping("/roster/{id}")
    public ResponseEntity<Void> deleteRoster(@PathVariable UUID id) {
        rosterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/timeoff")
    public ResponseEntity<PharmacistTimeOff> addTimeOff(@RequestBody @Valid TimeOffRequest body) {
        PharmacistTimeOff timeOff = new PharmacistTimeOff();
        timeOff.setId(UUID.randomUUID());
        timeOff.setPharmacistId(body.pharmacistId());
        timeOff.setBranchId(body.branchId());
        timeOff.setStartAt(body.startAt());
        timeOff.setEndAt(body.endAt());
        timeOff.setReason(body.reason());
        return ResponseEntity.ok(timeOffRepository.save(timeOff));
    }

    @DeleteMapping("/timeoff/{id}")
    public ResponseEntity<Void> deleteTimeOff(@PathVariable UUID id) {
        timeOffRepository.deleteById(id);
        return ResponseEntity.noContent().build();
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