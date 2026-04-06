package com.backend.pharmacist.service;

import com.backend.pharmacist.api.dto.*;
import com.backend.pharmacist.cache.CacheHelper;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PharmacistService {

    private final PharmacistRepository pharmacistRepo;
    private final PharmacistShiftRepository shiftRepo;
    private final ObjectMapper mapper;
    private final BranchClient branchClient;
    private final CacheHelper cacheHelper;

    @Value("${cache.ttl-seconds:600}")
    private long cacheTtlSeconds;

    public PharmacistService(
            PharmacistRepository pharmacistRepo,
            PharmacistShiftRepository shiftRepo,
            ObjectMapper mapper,
            BranchClient branchClient,
            CacheHelper cacheHelper) {
        this.pharmacistRepo = pharmacistRepo;
        this.shiftRepo = shiftRepo;
        this.mapper = mapper;
        this.branchClient = branchClient;
        this.cacheHelper = cacheHelper;
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
            UUID branchId,
            String sortBy,
            String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy, sortDir));
        List<UUID> pharmacistIds = branchClient.getBranchPharmacistIds(branchId);
        if (branchId != null && (pharmacistIds == null || pharmacistIds.isEmpty())) {
            return Page.empty(pageable);
        }
        String cacheKey = "pharmacist:pharmacist:list:query:" + normalizeKey(query)
                + ":specialty:" + normalizeKey(specialty)
                + ":status:" + normalizeKey(status)
                + ":mode:" + normalizeKey(mode)
                + ":experience:" + normalizeKey(experience)
                + ":verified:" + (verified == null ? "_" : verified)
                + ":branch:" + (branchId == null ? "_" : branchId)
                + ":sortBy:" + normalizeKey(sortBy)
                + ":sortDir:" + normalizeKey(sortDir)
                + ":page:" + page
                + ":size:" + size;
        return cacheHelper.getOrSetCache(cacheKey, cacheTtlSeconds, () -> {
            Specification<Pharmacist> spec = buildSpec(query, specialty, status, mode, experience, verified,
                    pharmacistIds);
            return pharmacistRepo.findAll(spec, pageable).map(this::toResponse);
        });
    }

    public List<PharmacistResponse> listOnline(int limit, UUID branchId) {
        Pageable pageable = PageRequest.of(0, limit);
        List<UUID> pharmacistIds = branchClient.getBranchPharmacistIds(branchId);
        if (branchId != null && (pharmacistIds == null || pharmacistIds.isEmpty())) {
            return List.of();
        }
        String cacheKey = "pharmacist:pharmacist:list:online:limit:" + limit
                + ":branch:" + (branchId == null ? "_" : branchId);
        return cacheHelper.getOrSetCache(cacheKey, cacheTtlSeconds, () -> {
            Specification<Pharmacist> spec = buildSpec(null, null, "ONLINE", null, null, true, pharmacistIds);
            return pharmacistRepo.findAll(spec, pageable).map(this::toResponse).getContent();
        });
    }

    public PharmacistResponse get(UUID id) {
        return cacheHelper.getOrSetCache("pharmacist:pharmacist:detail:" + id, cacheTtlSeconds,
                () -> pharmacistRepo.findById(id)
                        .map(this::toResponse)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found")));
    }

    public PharmacistResponse create(UpsertPharmacistRequest request) {
        Pharmacist pharmacist = new Pharmacist();
        pharmacist.setId(UUID.randomUUID());
        applyRequest(pharmacist, request, true);
        pharmacistRepo.save(pharmacist);
        syncPrimaryBranch(pharmacist);
        invalidatePharmacistCaches(pharmacist.getId());
        return toResponse(pharmacist);
    }

    public PharmacistResponse update(UUID id, UpsertPharmacistRequest request) {
        Pharmacist pharmacist = pharmacistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
        applyRequest(pharmacist, request, false);
        pharmacistRepo.save(pharmacist);
        syncPrimaryBranch(pharmacist);
        invalidatePharmacistCaches(pharmacist.getId());
        return toResponse(pharmacist);
    }

    public PharmacistResponse getOwnProfile(UUID actorId) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return pharmacistRepo.findById(actorId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist profile not found"));
    }

    public PharmacistResponse upsertOwnProfile(UUID actorId, UpsertPharmacistRequest request) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Pharmacist pharmacist = pharmacistRepo.findById(actorId).orElseGet(() -> {
            Pharmacist created = new Pharmacist();
            created.setId(actorId);
            created.setVerified(false);
            created.setRating(0d);
            created.setReviewCount(0);
            created.setCreatedAt(Instant.now());
            return created;
        });
        applyRequest(pharmacist, request, pharmacist.getCreatedAt() == null);
        pharmacistRepo.save(pharmacist);
        syncPrimaryBranch(pharmacist);
        invalidatePharmacistCaches(pharmacist.getId());
        return toResponse(pharmacist);
    }

    public PharmacistResponse bootstrapProfile(UUID pharmacistId, InternalPharmacistBootstrapRequest request) {
        if (pharmacistId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pharmacistId is required");
        }

        Pharmacist pharmacist = pharmacistRepo.findById(pharmacistId).orElseGet(() -> {
            Pharmacist created = new Pharmacist();
            created.setId(pharmacistId);
            created.setVerified(false);
            created.setRating(0d);
            created.setReviewCount(0);
            created.setStatus("OFFLINE");
            created.setExperienceYears(0);
            created.setSpecialty("general");
            created.setCreatedAt(Instant.now());
            return created;
        });

        if (StringUtils.hasText(request == null ? null : request.name())) {
            pharmacist.setName(request.name().trim());
        } else if (!StringUtils.hasText(pharmacist.getName())) {
            pharmacist.setName(pharmacist.getEmail());
        }

        if (StringUtils.hasText(request == null ? null : request.email())) {
            pharmacist.setEmail(request.email().trim());
        }
        if (StringUtils.hasText(request == null ? null : request.phone())) {
            pharmacist.setPhone(request.phone().trim());
        }
        if (StringUtils.hasText(request == null ? null : request.avatarUrl())) {
            pharmacist.setAvatarUrl(request.avatarUrl().trim());
        }
        if (StringUtils.hasText(request == null ? null : request.specialty())) {
            pharmacist.setSpecialty(request.specialty().trim());
        } else if (!StringUtils.hasText(pharmacist.getSpecialty())) {
            pharmacist.setSpecialty("general");
        }
        if (pharmacist.getRating() == null) {
            pharmacist.setRating(0d);
        }
        if (pharmacist.getReviewCount() == null) {
            pharmacist.setReviewCount(0);
        }
        if (!StringUtils.hasText(pharmacist.getStatus())) {
            pharmacist.setStatus("OFFLINE");
        }
        if (pharmacist.getCreatedAt() == null) {
            pharmacist.setCreatedAt(Instant.now());
        }
        pharmacist.setUpdatedAt(Instant.now());

        pharmacistRepo.save(pharmacist);
        syncPrimaryBranch(pharmacist);
        invalidatePharmacistCaches(pharmacist.getId());
        return toResponse(pharmacist);
    }

    public void delete(UUID id) {
        if (!pharmacistRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found");
        }
        pharmacistRepo.deleteById(id);
        branchClient.assignPrimaryBranch(id, null, "PHARMACIST");
        invalidatePharmacistCaches(id);
    }

    public PharmacistResponse updateVerification(UUID id, boolean verified) {
        Pharmacist pharmacist = pharmacistRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pharmacist not found"));
        pharmacist.setVerified(verified);
        pharmacist.setUpdatedAt(Instant.now());
        pharmacistRepo.save(pharmacist);
        invalidatePharmacistCaches(pharmacist.getId());
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
        invalidatePharmacistCaches(pharmacist.getId());
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
        pharmacist.setBranchId(request.branchId());
        Instant now = Instant.now();
        if (isCreate) {
            pharmacist.setCreatedAt(now);
        }
        pharmacist.setUpdatedAt(now);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : "_";
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        String requestedField = StringUtils.hasText(sortBy) ? sortBy.trim() : "createdAt";
        String requestedDirection = StringUtils.hasText(sortDir) ? sortDir.trim() : "desc";

        String property = switch (requestedField.toLowerCase(Locale.ROOT)) {
            case "name" -> "name";
            case "updatedat" -> "updatedAt";
            case "experienceyears" -> "experienceYears";
            case "rating" -> "rating";
            default -> "createdAt";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(requestedDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void invalidatePharmacistCaches(UUID pharmacistId) {
        cacheHelper.evictByPattern("pharmacist:pharmacist:list:*");
        if (pharmacistId != null) {
            cacheHelper.evict("pharmacist:pharmacist:detail:" + pharmacistId);
        }
    }

    private PharmacistResponse toResponse(Pharmacist pharmacist) {
        UUID branchId = pharmacist.getBranchId() != null
                ? pharmacist.getBranchId()
                : branchClient.getPrimaryBranchId(pharmacist.getId(), "PHARMACIST");
        BranchClient.BranchSummary branch = branchClient.getBranch(branchId);
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
                pharmacist.getLicenseNumber(),
                branchId,
                branch == null ? null : branch.name(),
                pharmacist.getCreatedAt(),
                pharmacist.getUpdatedAt());
    }

    private void syncPrimaryBranch(Pharmacist pharmacist) {
        branchClient.assignPrimaryBranch(pharmacist.getId(), pharmacist.getBranchId(), "PHARMACIST");
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
