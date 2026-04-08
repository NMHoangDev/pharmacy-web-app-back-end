package com.backend.branch.api;

import com.backend.branch.api.dto.*;
import com.backend.branch.model.BranchHoliday;
import com.backend.branch.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/branches")
public class BranchAdminApi {

    private final BranchService branchService;

    public BranchAdminApi(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<List<BranchSummaryResponse>> list(
            @RequestParam(name = "status", required = false) String status) {
        return ResponseEntity.ok(branchService.listBranches(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchSummaryResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getBranch(id));
    }

    @GetMapping("/{id}/settings")
    public ResponseEntity<BranchSettingsSummaryResponse> settings(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getInternalSettings(id));
    }

    @GetMapping("/{id}/hours")
    public ResponseEntity<BranchHoursRequest> hours(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getHours(id));
    }

    @GetMapping("/{id}/holidays")
    public ResponseEntity<List<BranchHoliday>> holidays(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.listHolidays(id));
    }

    @PostMapping
    public ResponseEntity<BranchSummaryResponse> create(@RequestBody @Valid BranchCreateRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.createBranch(req, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchSummaryResponse> update(@PathVariable UUID id,
            @RequestBody @Valid BranchUpdateRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.updateBranch(id, req, actor));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BranchSummaryResponse> updateStatus(@PathVariable UUID id,
            @RequestBody @Valid BranchStatusRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.updateStatus(id, req, actor));
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<BranchSettingsSummaryResponse> upsertSettings(@PathVariable UUID id,
            @RequestBody @Valid BranchSettingsRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.upsertSettings(id, req, actor));
    }

    @PutMapping("/{id}/hours")
    public ResponseEntity<BranchHoursRequest> upsertHours(@PathVariable UUID id,
            @RequestBody @Valid BranchHoursRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.upsertHours(id, req, actor));
    }

    @PostMapping("/{id}/holidays")
    public ResponseEntity<BranchHoliday> addHoliday(@PathVariable UUID id,
            @RequestBody @Valid BranchHolidayRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.addHoliday(id, req, actor));
    }

    @DeleteMapping("/{id}/holidays/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable UUID id,
            @PathVariable UUID holidayId,
            @RequestParam(name = "actor", required = false) String actor) {
        branchService.deleteHoliday(id, holidayId, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<BranchStaffResponse> assignStaff(@PathVariable UUID id,
            @RequestBody @Valid BranchStaffRequest req,
            @RequestParam(name = "actor", required = false) String actor) {
        return ResponseEntity.ok(branchService.assignStaff(id, req, actor));
    }

    @DeleteMapping("/{id}/staff/{userId}")
    public ResponseEntity<Void> removeStaff(@PathVariable UUID id,
            @PathVariable UUID userId,
            @RequestParam(name = "actor", required = false) String actor) {
        branchService.removeStaff(id, userId, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<BranchStaffResponse>> listStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.listStaff(id, null));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<BranchAuditResponse>> audit(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.listAudit(id));
    }
}
