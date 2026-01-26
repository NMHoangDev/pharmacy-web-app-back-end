package com.backend.consultation.api;

import com.backend.consultation.api.dto.AppointmentResponse;
import com.backend.consultation.api.dto.AvailableSlotResponse;
import com.backend.consultation.api.dto.CreateAppointmentRequest;
import com.backend.consultation.api.dto.UpdateAppointmentStatusRequest;
import com.backend.consultation.service.ConsultationAppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationAppointmentApi {

    private final ConsultationAppointmentService service;

    public ConsultationAppointmentApi(ConsultationAppointmentService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ok";
    }

    @GetMapping
    public Page<AppointmentResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID pharmacistId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(query, pharmacistId, userId, status, mode, from, to, page, size);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public AppointmentResponse create(@RequestBody CreateAppointmentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.create(request, authorization);
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(@PathVariable UUID id,
            @RequestBody UpdateAppointmentStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @GetMapping("/pharmacists/{pharmacistId}/slots")
    public List<AvailableSlotResponse> getSlots(
            @PathVariable UUID pharmacistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "30") int slotMinutes,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return service.getAvailableSlots(pharmacistId, from, to, authorization);
    }
}
