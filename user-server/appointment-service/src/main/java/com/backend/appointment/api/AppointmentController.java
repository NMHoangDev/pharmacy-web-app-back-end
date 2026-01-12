package com.backend.appointment.api;

import com.backend.appointment.api.dto.AppointmentRequest;
import com.backend.appointment.api.dto.AppointmentResponse;
import com.backend.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("appointment-service ok");
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@RequestBody @Valid AppointmentRequest req) {
        return ResponseEntity.ok(service.create(req));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AppointmentResponse>> byUser(@PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.listByUser(userId, page, size));
    }

    @GetMapping("/pharmacist/{pharmacistId}")
    public ResponseEntity<Page<AppointmentResponse>> byPharmacist(@PathVariable UUID pharmacistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.listByPharmacist(pharmacistId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(service.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}
