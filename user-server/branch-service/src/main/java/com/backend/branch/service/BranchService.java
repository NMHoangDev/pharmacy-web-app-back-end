package com.backend.branch.service;

import com.backend.branch.api.dto.*;
import com.backend.branch.event.BranchEventPayload;
import com.backend.branch.event.BranchEventPublisher;
import com.backend.branch.model.*;
import com.backend.branch.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class BranchService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9-]{2,64}$");

    private final BranchRepository branchRepository;
    private final BranchSettingsRepository branchSettingsRepository;
    private final BranchHoursRepository branchHoursRepository;
    private final BranchHolidayRepository branchHolidayRepository;
    private final BranchStaffRepository branchStaffRepository;
    private final BranchAuditLogRepository branchAuditLogRepository;
    private final BranchEventPublisher eventPublisher;

    public BranchService(BranchRepository branchRepository,
            BranchSettingsRepository branchSettingsRepository,
            BranchHoursRepository branchHoursRepository,
            BranchHolidayRepository branchHolidayRepository,
            BranchStaffRepository branchStaffRepository,
            BranchAuditLogRepository branchAuditLogRepository,
            BranchEventPublisher eventPublisher) {
        this.branchRepository = branchRepository;
        this.branchSettingsRepository = branchSettingsRepository;
        this.branchHoursRepository = branchHoursRepository;
        this.branchHolidayRepository = branchHolidayRepository;
        this.branchStaffRepository = branchStaffRepository;
        this.branchAuditLogRepository = branchAuditLogRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<BranchSummaryResponse> listActiveBranches() {
        return branchRepository.findByStatus("ACTIVE").stream()
                .map(this::toSummary)
                .toList();
    }

    public List<BranchSummaryResponse> listBranches(String status) {
        if (status == null || status.isBlank()) {
            return branchRepository.findAll().stream().map(this::toSummary).toList();
        }
        String normalized = normalizeStatus(status);
        return branchRepository.findByStatus(normalized).stream().map(this::toSummary).toList();
    }

    public BranchSummaryResponse getBranch(UUID id) {
        return toSummary(findBranch(id));
    }

    public BranchInternalResponse getInternal(UUID id) {
        Branch branch = findBranch(id);
        return new BranchInternalResponse(branch.getId(), branch.getCode(), branch.getName(), branch.getStatus(),
                branch.getTimezone());
    }

    public List<BranchInternalResponse> listInternalActive() {
        return branchRepository.findByStatus("ACTIVE").stream()
                .map(b -> new BranchInternalResponse(b.getId(), b.getCode(), b.getName(), b.getStatus(),
                        b.getTimezone()))
                .toList();
    }

    public BranchSettingsSummaryResponse getInternalSettings(UUID branchId) {
        Objects.requireNonNull(branchId, "branchId");
        BranchSettings settings = branchSettingsRepository.findById(branchId)
                .orElseGet(() -> defaultSettings(branchId));
        return toSettingsSummary(settings);
    }

    public BranchSummaryResponse createBranch(BranchCreateRequest req, String actor) {
        validateCode(req.code());
        if (branchRepository.findByCode(req.code()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã chi nhánh đã tồn tại");
        }
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID());
        branch.setCode(req.code().toLowerCase());
        branch.setName(req.name());
        branch.setStatus(normalizeStatus(req.status()));
        branch.setAddressLine(req.addressLine());
        branch.setWard(req.ward());
        branch.setDistrict(req.district());
        branch.setCity(req.city());
        branch.setProvince(req.province());
        branch.setCountry(req.country());
        branch.setLatitude(req.latitude());
        branch.setLongitude(req.longitude());
        branch.setPhone(req.phone());
        branch.setEmail(req.email());
        branch.setTimezone(req.timezone());
        branch.setNotes(req.notes());
        branch.setCoverImageUrl(req.coverImageUrl());
        branch.setCreatedAt(Instant.now());
        branch.setUpdatedAt(Instant.now());
        Branch saved = branchRepository.save(branch);

        BranchSettings settings = Objects.requireNonNull(defaultSettings(saved.getId()), "settings");
        branchSettingsRepository.save(settings);
        BranchHours hours = new BranchHours();
        hours.setBranchId(saved.getId());
        hours.setWeeklyHoursJson("{}");
        branchHoursRepository.save(hours);

        logAudit(saved.getId(), actor, "CREATE", "BRANCH", null, null);
        eventPublisher.publishBranchCreated(new BranchEventPayload(saved.getId(), saved.getCode(), saved.getName(),
                saved.getStatus(), saved.getUpdatedAt()));
        return toSummary(saved);
    }

    public BranchSummaryResponse updateBranch(UUID id, BranchUpdateRequest req, String actor) {
        Branch branch = findBranch(id);
        branch.setName(req.name());
        branch.setStatus(normalizeStatus(req.status()));
        branch.setAddressLine(req.addressLine());
        branch.setWard(req.ward());
        branch.setDistrict(req.district());
        branch.setCity(req.city());
        branch.setProvince(req.province());
        branch.setCountry(req.country());
        branch.setLatitude(req.latitude());
        branch.setLongitude(req.longitude());
        branch.setPhone(req.phone());
        branch.setEmail(req.email());
        branch.setTimezone(req.timezone());
        branch.setNotes(req.notes());
        branch.setCoverImageUrl(req.coverImageUrl());
        branch.setUpdatedAt(Instant.now());
        Branch saved = branchRepository.save(branch);
        logAudit(saved.getId(), actor, "UPDATE", "BRANCH", null, null);
        eventPublisher.publishBranchUpdated(new BranchEventPayload(saved.getId(), saved.getCode(), saved.getName(),
                saved.getStatus(), saved.getUpdatedAt()));
        return toSummary(saved);
    }

    public BranchSummaryResponse updateStatus(UUID id, BranchStatusRequest req, String actor) {
        Branch branch = findBranch(id);
        branch.setStatus(normalizeStatus(req.status()));
        branch.setUpdatedAt(Instant.now());
        Branch saved = branchRepository.save(branch);
        logAudit(saved.getId(), actor, "STATUS", "BRANCH", null, null);
        eventPublisher
                .publishBranchStatusChanged(new BranchEventPayload(saved.getId(), saved.getCode(), saved.getName(),
                        saved.getStatus(), saved.getUpdatedAt()));
        return toSummary(saved);
    }

    public BranchSettingsSummaryResponse upsertSettings(UUID branchId, BranchSettingsRequest req, String actor) {
        Objects.requireNonNull(branchId, "branchId");
        validateSlotDuration(req.slotDurationMinutes());
        BranchSettings settings = branchSettingsRepository.findById(branchId)
                .orElseGet(() -> defaultSettings(branchId));
        settings.setSlotDurationMinutes(req.slotDurationMinutes());
        settings.setBufferBeforeMinutes(valueOrZero(req.bufferBeforeMinutes()));
        settings.setBufferAfterMinutes(valueOrZero(req.bufferAfterMinutes()));
        settings.setLeadTimeMinutes(valueOrZero(req.leadTimeMinutes()));
        settings.setCutoffTime(req.cutoffTime());
        settings.setMaxBookingsPerPharmacistPerDay(req.maxBookingsPerPharmacistPerDay());
        settings.setMaxBookingsPerCustomerPerWeek(req.maxBookingsPerCustomerPerWeek());
        settings.setChannelsJson(req.channelsJson());
        settings.setPricingJson(req.pricingJson());
        settings.setPickupEnabled(req.pickupEnabled() != null && req.pickupEnabled());
        settings.setDeliveryEnabled(req.deliveryEnabled() != null && req.deliveryEnabled());
        settings.setDeliveryZonesJson(req.deliveryZonesJson());
        settings.setShippingFeeRulesJson(req.shippingFeeRulesJson());
        settings.setDefaultWarehouseCode(req.defaultWarehouseCode());
        settings.setAllowNegativeStock(req.allowNegativeStock() != null && req.allowNegativeStock());
        settings.setDefaultReorderPoint(req.defaultReorderPoint() == null ? 20 : req.defaultReorderPoint());
        settings.setEnableFefo(req.enableFefo() == null || req.enableFefo());
        settings.setUpdatedAt(Instant.now());
        BranchSettings saved = branchSettingsRepository.save(settings);
        logAudit(branchId, actor, "UPDATE", "SETTINGS", null, null);
        return toSettingsSummary(saved);
    }

    public BranchHoursRequest upsertHours(UUID branchId, BranchHoursRequest req, String actor) {
        Objects.requireNonNull(branchId, "branchId");
        BranchHours hours = branchHoursRepository.findById(branchId).orElseGet(() -> {
            BranchHours h = new BranchHours();
            h.setBranchId(branchId);
            return h;
        });
        hours.setWeeklyHoursJson(req.weeklyHoursJson());
        hours.setLunchBreakJson(req.lunchBreakJson());
        branchHoursRepository.save(hours);
        logAudit(branchId, actor, "UPDATE", "HOURS", null, null);
        return req;
    }

    public BranchHoursRequest getHours(UUID branchId) {
        Objects.requireNonNull(branchId, "branchId");
        BranchHours hours = branchHoursRepository.findById(branchId).orElseGet(() -> {
            BranchHours h = new BranchHours();
            h.setBranchId(branchId);
            h.setWeeklyHoursJson("{}");
            h.setLunchBreakJson(null);
            return h;
        });

        String weekly = hours.getWeeklyHoursJson();
        if (weekly == null || weekly.isBlank())
            weekly = "{}";
        return new BranchHoursRequest(weekly, hours.getLunchBreakJson());
    }

    public BranchHoliday addHoliday(UUID branchId, BranchHolidayRequest req, String actor) {
        Objects.requireNonNull(branchId, "branchId");
        BranchHoliday holiday = new BranchHoliday();
        holiday.setId(UUID.randomUUID());
        holiday.setBranchId(branchId);
        holiday.setDate(req.date());
        holiday.setType(req.type());
        holiday.setOpenTime(req.openTime());
        holiday.setCloseTime(req.closeTime());
        holiday.setNote(req.note());
        holiday.setCreatedAt(Instant.now());
        branchHolidayRepository.save(holiday);
        logAudit(branchId, actor, "CREATE", "HOLIDAY", null, null);
        return holiday;
    }

    public List<BranchHoliday> listHolidays(UUID branchId) {
        Objects.requireNonNull(branchId, "branchId");
        return branchHolidayRepository.findByBranchId(branchId);
    }

    public void deleteHoliday(UUID branchId, UUID holidayId, String actor) {
        Objects.requireNonNull(branchId, "branchId");
        Objects.requireNonNull(holidayId, "holidayId");
        BranchHoliday holiday = branchHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));
        if (!Objects.equals(holiday.getBranchId(), branchId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Holiday does not belong to branch");
        }
        branchHolidayRepository.deleteById(holidayId);
        logAudit(branchId, actor, "DELETE", "HOLIDAY", null, null);
    }

    public BranchStaffResponse assignStaff(UUID branchId, BranchStaffRequest req, String actor) {
        BranchStaff staff = new BranchStaff();
        staff.setId(new BranchStaffId(branchId, req.userId()));
        staff.setRole(req.role());
        staff.setSkillsJson(req.skillsJson());
        staff.setActive(req.active() == null || req.active());
        staff.setCreatedAt(Instant.now());
        branchStaffRepository.save(staff);
        logAudit(branchId, actor, "ASSIGN", "STAFF", null, null);
        return toStaffResponse(staff);
    }

    public void removeStaff(UUID branchId, UUID userId, String actor) {
        BranchStaffId id = new BranchStaffId(branchId, userId);
        if (!branchStaffRepository.existsById(id)) {
            return;
        }
        branchStaffRepository.deleteById(id);
        logAudit(branchId, actor, "REMOVE", "STAFF", null, null);
    }

    public List<BranchStaffResponse> listStaff(UUID branchId, String role) {
        List<BranchStaff> staff = role == null || role.isBlank()
                ? branchStaffRepository.findByIdBranchId(branchId)
                : branchStaffRepository.findByIdBranchIdAndRole(branchId, role);
        return staff.stream().map(this::toStaffResponse).toList();
    }

    public List<BranchAuditResponse> listAudit(UUID branchId) {
        return branchAuditLogRepository.findByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .map(this::toAuditResponse)
                .toList();
    }

    private Branch findBranch(UUID id) {
        return branchRepository.findById(Objects.requireNonNull(id, "id"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    private BranchSummaryResponse toSummary(Branch branch) {
        return new BranchSummaryResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getStatus(),
                branch.getAddressLine(),
                branch.getWard(),
                branch.getDistrict(),
                branch.getCity(),
                branch.getProvince(),
                branch.getCountry(),
                branch.getPhone(),
                branch.getTimezone());
    }

    private BranchSettingsSummaryResponse toSettingsSummary(BranchSettings settings) {
        return new BranchSettingsSummaryResponse(
                settings.getBranchId(),
                settings.getSlotDurationMinutes(),
                settings.getBufferBeforeMinutes(),
                settings.getBufferAfterMinutes(),
                settings.getLeadTimeMinutes(),
                settings.getCutoffTime(),
                settings.getMaxBookingsPerPharmacistPerDay(),
                settings.getMaxBookingsPerCustomerPerWeek(),
                settings.getChannelsJson(),
                settings.getPricingJson(),
                settings.isPickupEnabled(),
                settings.isDeliveryEnabled(),
                settings.getDeliveryZonesJson(),
                settings.getShippingFeeRulesJson(),
                settings.getDefaultWarehouseCode(),
                settings.isAllowNegativeStock(),
                settings.getDefaultReorderPoint(),
                settings.isEnableFefo());
    }

    private BranchStaffResponse toStaffResponse(BranchStaff staff) {
        return new BranchStaffResponse(
                staff.getId().getBranchId(),
                staff.getId().getUserId(),
                staff.getRole(),
                staff.getSkillsJson(),
                staff.isActive(),
                staff.getCreatedAt());
    }

    private BranchAuditResponse toAuditResponse(BranchAuditLog log) {
        return new BranchAuditResponse(
                log.getId(),
                log.getBranchId(),
                log.getActor(),
                log.getAction(),
                log.getEntity(),
                log.getBeforeJson(),
                log.getAfterJson(),
                log.getCreatedAt());
    }

    private void logAudit(UUID branchId, String actor, String action, String entity, String before, String after) {
        BranchAuditLog log = new BranchAuditLog();
        log.setId(UUID.randomUUID());
        log.setBranchId(branchId);
        log.setActor(actor);
        log.setAction(action);
        log.setEntity(entity);
        log.setBeforeJson(before);
        log.setAfterJson(after);
        log.setCreatedAt(Instant.now());
        branchAuditLogRepository.save(log);
    }

    private void validateCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code.toLowerCase()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã chi nhánh không hợp lệ");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái chi nhánh không hợp lệ");
        }
        return normalized;
    }

    private void validateSlotDuration(Integer slotDuration) {
        if (slotDuration == null
                || !(slotDuration == 15 || slotDuration == 20 || slotDuration == 30 || slotDuration == 60)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "slotDuration không hợp lệ");
        }
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private BranchSettings defaultSettings(UUID branchId) {
        BranchSettings settings = new BranchSettings();
        settings.setBranchId(branchId);
        settings.setSlotDurationMinutes(30);
        settings.setBufferBeforeMinutes(0);
        settings.setBufferAfterMinutes(0);
        settings.setLeadTimeMinutes(0);
        settings.setPickupEnabled(true);
        settings.setDeliveryEnabled(false);
        settings.setAllowNegativeStock(false);
        settings.setDefaultReorderPoint(20);
        settings.setEnableFefo(true);
        settings.setUpdatedAt(Instant.now());
        return settings;
    }
}
