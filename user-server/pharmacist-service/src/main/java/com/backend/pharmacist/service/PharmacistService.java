package com.backend.pharmacist.service;

import com.backend.pharmacist.api.dto.*;
import com.backend.pharmacist.model.Pharmacist;
import com.backend.pharmacist.model.PharmacistShift;
import com.backend.pharmacist.repo.PharmacistRepository;
import com.backend.pharmacist.repo.PharmacistShiftRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PharmacistService {

    private final PharmacistRepository pharmacistRepo;
    private final PharmacistShiftRepository shiftRepo;
    private final ObjectMapper mapper;
    private final BranchClient branchClient;

    public PharmacistService(
            PharmacistRepository pharmacistRepo,
            PharmacistShiftRepository shiftRepo,
            ObjectMapper mapper,
            BranchClient branchClient) {
        this.pharmacistRepo = pharmacistRepo;
        this.shiftRepo = shiftRepo;
        this.mapper = mapper;
        this.branchClient = branchClient;
    }

    public Page<PharmacistResponse> list(
            String query,
            String specialty,
            String status,
            String mode,
            String experience,
            Boolean verified,
            int page,
            int size,
            UUID branchId) {
        Pageable pageable = PageRequest.of(page, size);
        List<UUID> pharmacistIds = branchClient.getBranchPharmacistIds(branchId);
        if (branchId != null && (pharmacistIds == null || pharmacistIds.isEmpty())) {
            return Page.empty(pageable);
        }
        Specification<Pharmacist> spec = buildSpec(query, specialty, status, mode, experience, verified,
                pharmacistIds);
        return pharmacistRepo.findAll(spec, pageable).map(this::toResponse);
    }

    public List<PharmacistResponse> listOnline(int limit, UUID branchId) {
        Pageable pageable = PageRequest.of(0, limit);
        List<UUID> pharmacistIds = branchClient.getBranchPharmacistIds(branchId);
        if (branchId != null && (pharmacistIds == null || pharmacistIds.isEmpty())) {
            return List.of();
        }
        Specification<Pharmacist> spec = buildSpec(null, null, "ONLINE", null, null, true, pharmacistIds);
        return pharmacistRepo.findAll(spec, pageable).map(this::toResponse).getContent();
    }

    public PharmacistResponse get(UUID id) {
        return pharmacistRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
    }

    public PharmacistResponse create(UpsertPharmacistRequest request) {
        Pharmacist pharmacist = new Pharmacist();
        pharmacist.setId(UUID.randomUUID());
        applyRequest(pharmacist, request, true);
        pharmacistRepo.save(pharmacist);
        return toResponse(pharmacist);
    }

    public PharmacistResponse update(UUID id, UpsertPharmacistRequest request) {
        Pharmacist pharmacist = pharmacistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
        applyRequest(pharmacist, request, false);
        pharmacistRepo.save(pharmacist);
        return toResponse(pharmacist);
    }

    public void delete(UUID id) {
        if (!pharmacistRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found");
        }
        pharmacistRepo.deleteById(id);
    }

    public PharmacistResponse updateVerification(UUID id, boolean verified) {
        Pharmacist pharmacist = pharmacistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
        pharmacist.setVerified(verified);
        pharmacist.setUpdatedAt(Instant.now());
        pharmacistRepo.save(pharmacist);
        return toResponse(pharmacist);
    }

    public PharmacistResponse updateStatus(UUID id, UpdateStatusRequest request) {
        Pharmacist pharmacist = pharmacistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
        if (request == null || !StringUtils.hasText(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        String normalizedStatus = request.status().trim().toUpperCase();
        if (!"ONLINE".equals(normalizedStatus) && !"OFFLINE".equals(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be ONLINE or OFFLINE");
        }
        pharmacist.setStatus(normalizedStatus);
        if (StringUtils.hasText(request.availability())) {
            pharmacist.setAvailability(request.availability());
        }
        pharmacist.setUpdatedAt(Instant.now());
        pharmacistRepo.save(pharmacist);
        return toResponse(pharmacist);
    }

    public ShiftResponse createShift(UUID pharmacistId, ShiftRequest request) {
        ensurePharmacistExists(pharmacistId);
        PharmacistShift shift = new PharmacistShift();
        shift.setId(UUID.randomUUID());
        shift.setPharmacistId(pharmacistId);
        shift.setStartAt(request.startAt());
        shift.setEndAt(request.endAt());
        shift.setNote(request.note());
        shift.setStatus("ACTIVE");
        shift.setCreatedAt(Instant.now());
        shiftRepo.save(shift);
        return toShiftResponse(shift);
    }

    public List<ShiftResponse> listShifts(UUID pharmacistId, LocalDateTime from, LocalDateTime to) {
        ensurePharmacistExists(pharmacistId);
        List<PharmacistShift> shifts;
        if (from != null && to != null) {
            shifts = shiftRepo.findByPharmacistIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(pharmacistId, from,
                    to);
        } else {
            shifts = shiftRepo.findByPharmacistId(pharmacistId);
        }
        return shifts.stream().map(this::toShiftResponse).toList();
    }

    private void ensurePharmacistExists(UUID pharmacistId) {
        if (!pharmacistRepo.existsById(pharmacistId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found");
        }
    }

    private Specification<Pharmacist> buildSpec(
            String query,
            String specialty,
            String status,
            String mode,
            String experience,
            Boolean verified,
            List<UUID> pharmacistIds) {
        return (root, q, cb) -> {
            var predicates = cb.conjunction();
            if (pharmacistIds != null && !pharmacistIds.isEmpty()) {
                predicates.getExpressions().add(root.get("id").in(pharmacistIds));
            }
            if (StringUtils.hasText(query)) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.getExpressions().add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), like),
                                cb.like(cb.lower(root.get("code")), like),
                                cb.like(cb.lower(root.get("email")), like),
                                cb.like(cb.lower(root.get("phone")), like),
                                cb.like(cb.lower(root.get("licenseNumber")), like)));
            }
            if (StringUtils.hasText(specialty) && !"all".equalsIgnoreCase(specialty)) {
                predicates.getExpressions().add(cb.equal(cb.lower(root.get("specialty")), specialty.toLowerCase()));
            }
            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
                predicates.getExpressions().add(cb.equal(cb.upper(root.get("status")), status.toUpperCase()));
            }
            if (StringUtils.hasText(mode) && !"all".equalsIgnoreCase(mode)) {
                String modeValue = "\"" + mode.toUpperCase() + "\"";
                predicates.getExpressions().add(cb.like(cb.lower(root.get("consultationModesJson")),
                        "%" + modeValue.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(experience) && !"all".equalsIgnoreCase(experience)) {
                switch (experience) {
                    case "junior" -> predicates.getExpressions().add(cb.lessThan(root.get("experienceYears"), 3));
                    case "mid" -> predicates.getExpressions().add(cb.between(root.get("experienceYears"), 3, 5));
                    case "senior" -> predicates.getExpressions().add(cb.greaterThan(root.get("experienceYears"), 5));
                    default -> {
                    }
                }
            }
            if (verified != null) {
                predicates.getExpressions().add(cb.equal(root.get("verified"), verified));
            }
            return predicates;
        };
    }

    private void applyRequest(Pharmacist pharmacist, UpsertPharmacistRequest request, boolean isCreate) {
        pharmacist.setCode(normalize(request.code()));
        pharmacist.setName(StringUtils.hasText(request.name()) ? request.name().trim() : request.name());
        pharmacist.setEmail(normalize(request.email()));
        pharmacist.setPhone(normalize(request.phone()));
        pharmacist.setAvatarUrl(normalize(request.avatarUrl()));
        pharmacist.setSpecialty(StringUtils.hasText(request.specialty()) ? request.specialty().trim() : "clinical");
        pharmacist.setExperienceYears(request.experienceYears() == null ? 0 : request.experienceYears());
        pharmacist.setStatus(StringUtils.hasText(request.status()) ? request.status().toUpperCase() : "OFFLINE");
        pharmacist.setVerified(request.verified() != null && request.verified());
        pharmacist.setAvailability(normalize(request.availability()));
        pharmacist.setRating(request.rating());
        pharmacist.setReviewCount(request.reviewCount());
        pharmacist.setBio(normalize(request.bio()));
        pharmacist.setEducation(normalize(request.education()));
        pharmacist.setLanguagesJson(toJson(request.languages()));
        pharmacist.setWorkingDaysJson(toJson(request.workingDays()));
        pharmacist.setWorkingHours(normalize(request.workingHours()));
        pharmacist.setConsultationModesJson(toJson(request.consultationModes()));
        pharmacist.setLicenseNumber(normalize(request.licenseNumber()));
        Instant now = Instant.now();
        if (isCreate) {
            pharmacist.setCreatedAt(now);
        }
        pharmacist.setUpdatedAt(now);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private PharmacistResponse toResponse(Pharmacist pharmacist) {
        return new PharmacistResponse(
                pharmacist.getId(),
                pharmacist.getCode(),
                pharmacist.getName(),
                pharmacist.getEmail(),
                pharmacist.getPhone(),
                pharmacist.getAvatarUrl(),
                pharmacist.getSpecialty(),
                pharmacist.getExperienceYears(),
                pharmacist.getStatus(),
                pharmacist.isVerified(),
                pharmacist.getAvailability(),
                pharmacist.getRating(),
                pharmacist.getReviewCount(),
                pharmacist.getBio(),
                pharmacist.getEducation(),
                fromJson(pharmacist.getLanguagesJson()),
                fromJson(pharmacist.getWorkingDaysJson()),
                pharmacist.getWorkingHours(),
                fromJson(pharmacist.getConsultationModesJson()),
                pharmacist.getLicenseNumber());
    }

    private ShiftResponse toShiftResponse(PharmacistShift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getPharmacistId(),
                shift.getStartAt(),
                shift.getEndAt(),
                shift.getNote(),
                shift.getStatus());
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
