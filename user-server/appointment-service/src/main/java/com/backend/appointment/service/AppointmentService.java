package com.backend.appointment.service;

import com.backend.appointment.api.dto.AppointmentRequest;
import com.backend.appointment.api.dto.AppointmentResponse;
import com.backend.appointment.cache.CacheConstants;
import com.backend.appointment.cache.CacheHelper;
import com.backend.appointment.cache.CacheKeyBuilder;
import com.backend.appointment.messaging.AppointmentEventTypes;
import com.backend.appointment.model.Appointment;
import com.backend.appointment.model.AppointmentStatus;
import com.backend.appointment.model.Channel;
import com.backend.appointment.model.PharmacistRoster;
import com.backend.appointment.model.PharmacistTimeOff;
import com.backend.appointment.repo.AppointmentRepository;
import com.backend.appointment.repo.PharmacistRosterRepository;
import com.backend.appointment.repo.PharmacistTimeOffRepository;
import com.backend.appointment.security.SecurityUtils;
import com.backend.appointment.client.UserClient;
import com.backend.common.messaging.TopicNames;
import com.backend.common.model.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private final AppointmentRepository repo;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final com.backend.appointment.client.PharmacistClient pharmacistClient;
    private final UserClient userClient;
    private final PharmacistRosterRepository rosterRepository;
    private final PharmacistTimeOffRepository timeOffRepository;
    private final AppointmentAuditService auditService;
    private final BranchClient branchClient;
    private final CacheHelper cacheHelper;
    private final CacheKeyBuilder cacheKeyBuilder;

    @Value("${appointment.buffer-minutes:10}")
    private int bufferMinutes;

    public AppointmentService(AppointmentRepository repo, KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate,
            com.backend.appointment.client.PharmacistClient pharmacistClient,
            UserClient userClient,
            PharmacistRosterRepository rosterRepository,
            PharmacistTimeOffRepository timeOffRepository,
            AppointmentAuditService auditService,
            BranchClient branchClient,
            CacheHelper cacheHelper,
            CacheKeyBuilder cacheKeyBuilder) {
        this.repo = repo;
        this.kafkaTemplate = kafkaTemplate;
        this.pharmacistClient = pharmacistClient;
        this.userClient = userClient;
        this.rosterRepository = rosterRepository;
        this.timeOffRepository = timeOffRepository;
        this.auditService = auditService;
        this.branchClient = branchClient;
        this.cacheHelper = cacheHelper;
        this.cacheKeyBuilder = cacheKeyBuilder;
    }

    public AppointmentResponse create(AppointmentRequest req, String actorIp) {
        validateTime(req.startAt(), req.endAt());
        ensureAvailability(req.pharmacistId(), req.branchId(), req.startAt(), req.endAt());
        Appointment a = new Appointment();
        a.setId(UUID.randomUUID());
        a.setUserId(req.userId());
        a.setPharmacistId(req.pharmacistId());
        a.setBranchId(req.branchId());
        a.setStartAt(req.startAt());
        a.setEndAt(req.endAt());
        a.setChannel(req.channel() == null ? Channel.VIDEO : req.channel());
        a.setNotes(req.notes());
        a.setStatus(AppointmentStatus.REQUESTED);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "CREATE", null, a.getStatus().name(), null, getActorRole(), actorIp, null);
        publish(AppointmentEventTypes.APPOINTMENT_CREATED, toResponse(a, canViewNotes()));
        return toResponse(a, canViewNotes());
    }

    public Page<AppointmentResponse> listByUser(UUID userId, int page, int size) {
        boolean includeNotes = canViewNotesForUser(userId);
        Pageable pageable = PageRequest.of(page, size);
        return repo.findByUserId(userId, pageable).map(a -> toResponse(a, includeNotes));
    }

    public Page<AppointmentResponse> listByUser(UUID userId, UUID branchId, int page, int size) {
        boolean includeNotes = canViewNotesForUser(userId);
        Pageable pageable = PageRequest.of(page, size);
        if (branchId == null) {
            return repo.findByUserId(userId, pageable).map(a -> toResponse(a, includeNotes));
        }
        return repo.findByUserIdAndBranchId(userId, branchId, pageable)
                .map(a -> toResponse(a, includeNotes));
    }

    public Page<AppointmentResponse> listByPharmacist(UUID pharmacistId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.findByPharmacistId(pharmacistId, pageable).map(a -> toResponse(a, true));
    }

    public Page<AppointmentResponse> listByPharmacist(UUID pharmacistId, UUID branchId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (branchId == null) {
            return repo.findByPharmacistId(pharmacistId, pageable).map(a -> toResponse(a, true));
        }
        return repo.findByPharmacistIdAndBranchId(pharmacistId, branchId, pageable).map(a -> toResponse(a, true));
    }

    public AppointmentResponse get(UUID id, boolean includeNotes) {
        return toResponse(repo.findById(requireId(id)).orElseThrow(() -> notFound()), includeNotes);
    }

    public AppointmentResponse confirm(UUID id, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        if (a.getStatus() == AppointmentStatus.CONFIRMED) {
            return toResponse(a, canViewNotes());
        }
        transition(a, AppointmentStatus.CONFIRMED);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "CONFIRM", null, a.getStatus().name(), reason, getActorRole(), actorIp, null);
        publish(AppointmentEventTypes.APPOINTMENT_ACCEPTED, toResponse(a, canViewNotes()));
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse cancel(UUID id, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        if (a.getStatus() == AppointmentStatus.CANCELLED) {
            return toResponse(a, canViewNotes());
        }
        transition(a, AppointmentStatus.CANCELLED);
        a.setCancelReason(reason);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "CANCEL", null, a.getStatus().name(), reason, getActorRole(), actorIp, null);
        publish(SecurityUtils.isPharmacist() ? AppointmentEventTypes.APPOINTMENT_REJECTED
                : AppointmentEventTypes.APPOINTMENT_CANCELLED,
                toResponse(a, canViewNotes()));
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse start(UUID id, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        transition(a, AppointmentStatus.IN_PROGRESS);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "START", null, a.getStatus().name(), null, getActorRole(), actorIp, null);
        return toResponse(a, true);
    }

    public AppointmentResponse complete(UUID id, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        transition(a, AppointmentStatus.COMPLETED);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "COMPLETE", null, a.getStatus().name(), null, getActorRole(), actorIp, null);
        return toResponse(a, true);
    }

    public AppointmentResponse noShow(UUID id, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        transition(a, AppointmentStatus.NO_SHOW);
        a.setNoShowReason(reason);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "NO_SHOW", null, a.getStatus().name(), reason, getActorRole(), actorIp, null);
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse refund(UUID id, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        transition(a, AppointmentStatus.REFUNDED);
        a.setRefundReason(reason);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "REFUND", null, a.getStatus().name(), reason, getActorRole(), actorIp, null);
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse reschedule(UUID id, LocalDateTime start, LocalDateTime end, String reason,
            String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        validateTime(start, end);
        ensureAvailability(a.getPharmacistId(), a.getBranchId(), start, end);
        transition(a, AppointmentStatus.RESCHEDULED);
        a.setStartAt(start);
        a.setEndAt(end);
        a.setRescheduleReason(reason);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "RESCHEDULE", null, a.getStatus().name(), reason, getActorRole(), actorIp, null);
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse assign(UUID id, UUID pharmacistId, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        ensureAvailability(pharmacistId, a.getBranchId(), a.getStartAt(), a.getEndAt());
        a.setPharmacistId(pharmacistId);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "ASSIGN", null, a.getStatus().name(), reason, getActorRole(), actorIp,
                "pharmacistId=" + pharmacistId);
        return toResponse(a, canViewNotes());
    }

    public AppointmentResponse autoAssign(UUID id, String strategy, String actorIp) {
        Appointment appointment = repo.findById(requireId(id)).orElseThrow(this::notFound);
        java.util.List<UUID> candidates = appointment.getBranchId() == null
                ? rosterRepository.findDistinctPharmacistIds()
                : rosterRepository.findDistinctPharmacistIdsByBranchId(appointment.getBranchId());
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pharmacist roster available");
        }

        java.util.List<UUID> available = new java.util.ArrayList<>();
        for (UUID pharmacistId : candidates) {
            if (isAvailable(pharmacistId, appointment.getBranchId(), appointment.getStartAt(),
                    appointment.getEndAt())) {
                available.add(pharmacistId);
            }
        }

        if (available.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No available pharmacist for this slot");
        }

        UUID selected = available.get(0);
        if ("least_loaded".equalsIgnoreCase(strategy)) {
            selected = selectLeastLoaded(available, appointment.getBranchId(), appointment.getStartAt(),
                    appointment.getEndAt());
        }

        return assign(id, selected, "auto-assign:" + strategy, actorIp);
    }

    public AppointmentResponse updateStatusByAdmin(UUID id, String status, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        AppointmentStatus next;
        try {
            next = AppointmentStatus.valueOf(status);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        transition(a, next);
        a.setUpdatedAt(Instant.now());
        repo.save(a);
        invalidateAppointmentCaches(a);
        auditService.log(a.getId(), "STATUS_UPDATE", null, a.getStatus().name(), reason, getActorRole(), actorIp,
                "status=" + status);
        publish(AppointmentEventTypes.APPOINTMENT_STATUS_UPDATED, toResponse(a, true));
        return toResponse(a, true);
    }

    public void logBreakGlass(UUID id, String reason, String actorIp) {
        Appointment a = repo.findById(requireId(id)).orElseThrow(this::notFound);
        auditService.log(a.getId(), "BREAK_GLASS", a.getStatus().name(), a.getStatus().name(), reason, getActorRole(),
                actorIp, null);
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt must be after startAt");
        }
    }

    private void ensureAvailability(UUID pharmacistId, UUID branchId, LocalDateTime start, LocalDateTime end) {
        if (pharmacistId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pharmacistId required");
        }
        if (branchId != null) {
            if (!branchClient.isBranchActive(branchId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch is inactive or not found");
            }
            List<UUID> staff = branchClient.getBranchPharmacistIds(branchId);
            if (staff != null && !staff.isEmpty() && !staff.contains(pharmacistId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Pharmacist is not assigned to the branch");
            }
        }

        int buffer = Math.max(bufferMinutes, 0);
        LocalDateTime startBuffered = start.minusMinutes(buffer);
        LocalDateTime endBuffered = end.plusMinutes(buffer);

        java.util.List<AppointmentStatus> activeStatuses = java.util.List.of(
                AppointmentStatus.REQUESTED,
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.RESCHEDULED);

        boolean busy = branchId == null
                ? repo.existsByPharmacistIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        pharmacistId,
                        activeStatuses,
                        endBuffered,
                        startBuffered)
                : repo.existsByPharmacistIdAndBranchIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        pharmacistId,
                        branchId,
                        activeStatuses,
                        endBuffered,
                        startBuffered);
        if (busy) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pharmacist not available in the time window");
        }

        enforceRoster(pharmacistId, branchId, start, end);
        enforceTimeOff(pharmacistId, branchId, start, end);
    }

    public List<AppointmentResponse> listByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return repo.findByStartAtBetween(start, end)
                .stream()
                .map(a -> toResponse(a, true))
                .toList();
    }

    public List<AppointmentResponse> listByDate(LocalDate date, UUID branchId) {
        if (branchId == null) {
            return listByDate(date);
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return repo.findByStartAtBetweenAndBranchId(start, end, branchId)
                .stream()
                .map(a -> toResponse(a, true))
                .toList();
    }

    private AppointmentResponse toResponse(Appointment a, boolean includeNotes) {
        com.backend.appointment.client.dto.PharmacistPreviewDto pharmacist = null;
        String patientName = a.getFullName();
        String patientContact = a.getContact();
        try {
            if (a.getPharmacistId() != null) {
                pharmacist = pharmacistClient.getPharmacist(a.getPharmacistId());
            }
        } catch (Exception e) {
            // log error but don't fail
            // System.err.println("Failed to fetch pharmacist: " + e.getMessage());
        }

        boolean pharmacistView = SecurityUtils.isPharmacist();
        if (a.getUserId() != null && (pharmacistView || isBlank(patientName) || isBlank(patientContact))) {
            try {
                var user = userClient.getUser(a.getUserId());
                if (pharmacistView && !isBlank(user.fullName())) {
                    patientName = user.fullName();
                } else if (isBlank(patientName) && !isBlank(user.fullName())) {
                    patientName = user.fullName();
                }
                if (isBlank(patientContact) && !isBlank(user.phone())) {
                    patientContact = user.phone();
                }
            } catch (Exception ex) {
                log.warn("Failed to enrich appointment {} from user-service for user {}: {}",
                        a.getId(), a.getUserId(), ex.getMessage());
            }
        }

        return new AppointmentResponse(a.getId(), a.getUserId(), patientName, patientContact, a.getPharmacistId(),
                a.getBranchId(), a.getStartAt(), a.getEndAt(), a.getStatus(), a.getChannel(),
                includeNotes ? a.getNotes() : null,
                a.getCancelReason(), a.getRescheduleReason(), a.getRefundReason(), a.getNoShowReason(), pharmacist,
                a.getCreatedAt(), a.getUpdatedAt());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void publish(String type, AppointmentResponse payload) {
        try {
            kafkaTemplate.send(TopicNames.APPOINTMENT_EVENTS, EventEnvelope.of(type, "1", payload));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to publish appointment event: " + e.getMessage());
        }
    }

    private void transition(Appointment appointment, AppointmentStatus next) {
        AppointmentStatus current = appointment.getStatus();
        if (current == null) {
            appointment.setStatus(next);
            return;
        }

        boolean allowed = switch (current) {
            case REQUESTED, PENDING -> next == AppointmentStatus.CONFIRMED || next == AppointmentStatus.CANCELLED;
            case CONFIRMED -> next == AppointmentStatus.IN_PROGRESS || next == AppointmentStatus.CANCELLED
                    || next == AppointmentStatus.NO_SHOW || next == AppointmentStatus.RESCHEDULED;
            case IN_PROGRESS -> next == AppointmentStatus.COMPLETED || next == AppointmentStatus.NO_SHOW;
            case RESCHEDULED -> next == AppointmentStatus.CONFIRMED || next == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW, REFUNDED, DONE -> next == current;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition: " + current + " -> " + next);
        }

        appointment.setStatus(next);
    }

    private void enforceRoster(UUID pharmacistId, UUID branchId, LocalDateTime start, LocalDateTime end) {
        java.util.List<PharmacistRoster> rosters = branchId == null
                ? rosterRepository.findByPharmacistIdAndDayOfWeek(pharmacistId, start.getDayOfWeek().getValue())
                : rosterRepository.findByPharmacistIdAndBranchIdAndDayOfWeek(pharmacistId, branchId,
                        start.getDayOfWeek().getValue());
        if (rosters == null || rosters.isEmpty()) {
            return; // no roster configured -> allow
        }
        boolean within = rosters.stream().anyMatch(
                r -> !start.toLocalTime().isBefore(r.getStartTime()) && !end.toLocalTime().isAfter(r.getEndTime()));
        if (!within) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Outside pharmacist roster availability");
        }
    }

    private void enforceTimeOff(UUID pharmacistId, UUID branchId, LocalDateTime start, LocalDateTime end) {
        java.util.List<PharmacistTimeOff> timeOff = branchId == null
                ? timeOffRepository.findByPharmacistIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(pharmacistId,
                        end, start)
                : timeOffRepository.findByPharmacistIdAndBranchIdAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        pharmacistId, branchId, end, start);
        if (timeOff != null && !timeOff.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pharmacist is on time off");
        }
    }

    private boolean isAvailable(UUID pharmacistId, UUID branchId, LocalDateTime start, LocalDateTime end) {
        try {
            ensureAvailability(pharmacistId, branchId, start, end);
            return true;
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    private UUID selectLeastLoaded(java.util.List<UUID> pharmacists, UUID branchId, LocalDateTime start,
            LocalDateTime end) {
        java.util.List<AppointmentStatus> activeStatuses = java.util.List.of(
                AppointmentStatus.REQUESTED,
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.RESCHEDULED);
        UUID best = pharmacists.get(0);
        long bestCount = Long.MAX_VALUE;
        for (UUID pharmacistId : pharmacists) {
            long count = branchId == null
                    ? repo.countByPharmacistIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                            pharmacistId,
                            activeStatuses,
                            end,
                            start)
                    : repo.countByPharmacistIdAndBranchIdAndStatusInAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                            pharmacistId,
                            branchId,
                            activeStatuses,
                            end,
                            start);
            if (count < bestCount) {
                best = pharmacistId;
                bestCount = count;
            }
        }
        return best;
    }

    private boolean canViewNotes() {
        return SecurityUtils.isPharmacist();
    }

    private boolean canViewNotesForUser(UUID userId) {
        UUID actorId = SecurityUtils.getActorId();
        return SecurityUtils.isPharmacist() || (actorId != null && actorId.equals(userId));
    }

    private String getActorRole() {
        if (SecurityUtils.isAdmin()) {
            return "ADMIN";
        }
        if (SecurityUtils.isPharmacist()) {
            return "PHARMACIST";
        }
        return "USER";
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
    }

    private @NonNull UUID requireId(UUID id) {
        return Objects.requireNonNull(id, "id is required");
    }

    private void invalidateAppointmentCaches(Appointment appointment) {
        cacheHelper.evictByPattern(cacheKeyBuilder.pattern("appointment", "detail", appointment.getId()));
        if (appointment.getUserId() != null) {
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("appointment", "user", appointment.getUserId()));
        }
        if (appointment.getPharmacistId() != null) {
            cacheHelper.evictByPattern(
                    cacheKeyBuilder.pattern("appointment", "pharmacist", appointment.getPharmacistId()));
        }
    }
}
