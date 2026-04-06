package com.backend.branch.api;

import com.backend.branch.api.dto.BranchInternalResponse;
import com.backend.branch.api.dto.BranchPrimaryStaffRequest;
import com.backend.branch.api.dto.BranchSettingsSummaryResponse;
import com.backend.branch.api.dto.BranchStaffResponse;
import com.backend.branch.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/branches")
public class BranchInternalApi {

    private final BranchService branchService;

    public BranchInternalApi(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchInternalResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getInternal(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BranchInternalResponse>> listActive() {
        return ResponseEntity.ok(branchService.listInternalActive());
    }

    @GetMapping("/{id}/settings")
    public ResponseEntity<BranchSettingsSummaryResponse> settings(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getInternalSettings(id));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<BranchStaffResponse>> staff(
            @PathVariable UUID id,
            @RequestParam(name = "role", required = false) String role) {
        return ResponseEntity.ok(branchService.listStaff(id, role));
    }

    @GetMapping("/staff/{userId}/primary")
    public ResponseEntity<BranchStaffResponse> primaryStaff(
            @PathVariable UUID userId,
            @RequestParam(name = "role", required = false) String role) {
        BranchStaffResponse response = branchService.getPrimaryStaffAssignment(userId, role);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/staff/{userId}/primary")
    public ResponseEntity<BranchStaffResponse> assignPrimaryStaff(
            @PathVariable UUID userId,
            @RequestBody BranchPrimaryStaffRequest req) {
        return ResponseEntity.ok(branchService.assignPrimaryStaff(userId, req));
    }
}
