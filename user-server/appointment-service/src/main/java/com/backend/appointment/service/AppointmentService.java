package com.backend.appointment.service;

import com.backend.appointment.api.dto.AppointmentRequest;
import com.backend.appointment.api.dto.AppointmentResponse;
import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.model.Channel;
import com.backend.appointment.repo.AppointmentRepository;
import com.backend.common.messaging.EventTypes;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository repo;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    public AppointmentService(AppointmentRepository repo, KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.repo = repo;
        this.kafkaTemplate = kafkaTemplate;
    }

    public AppointmentResponse create(AppointmentRequest req) {
        validateTime(req.startAt(), req.endAt());
        ensureAvailability(req.pharmacistId(), req.startAt(), req.endAt());
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setUserId(req.userId());
        a.setPharmacistId(req.pharmacistId());
        a.setStartAt(req.startAt());
        a.setEndAt(req.endAt());
        a.setChannel(req.channel() == null ? Channel.VIDEO : req.channel());
        a.setNotes(req.notes());
        a.setStatus(AppointmentStatus.REQUESTED);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        publish(EventTypes.APPOINTMENT_CONFIRMED, toResponse(a)); // tentative event type until status changes
        return toResponse(a);
    }

    public Page<AppointmentResponse> listByUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.findByUserId(userId, pageable).map(this::toResponse);
    }

    public Page<AppointmentResponse> listByPharmacist(UUID pharmacistId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.findByPharmacistId(pharmacistId, pageable).map(this::toResponse);
    }

    public AppointmentResponse get(UUID id) {
        return toResponse(repo.findById(id).orElseThrow(() -> notFound()));
    }

    public AppointmentResponse confirm(UUID id) {
        Appointment a = repo.findById(id).orElseThrow(this::notFound);
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        publish(EventTypes.APPOINTMENT_CONFIRMED, toResponse(a));
        return toResponse(a);
    }

    public AppointmentResponse cancel(UUID id) {
        Appointment a = repo.findById(id).orElseThrow(this::notFound);
        a.setStatus(AppointmentStatus.CANCELLED);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        publish(EventTypes.APPOINTMENT_CONFIRMED, toResponse(a));
        return toResponse(a);
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }
    }

    private void ensureAvailability(UUID pharmacistId, LocalDateTime start, LocalDateTime end) {
        boolean busy = repo.existsByPharmacistIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(pharmacistId, end,
                start);
        if (busy) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pharmacist not available in the time window");
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getUserId(), a.getPharmacistId(), a.getStartAt(), a.getEndAt(),
                a.getStatus(), a.getChannel(), a.getNotes(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private void publish(String type, AppointmentResponse payload) {
        kafkaTemplate.send(TopicNames.APPOINTMENT_EVENTS, EventEnvelope.of(type, "1", payload));
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
    }
}
