package com.backend.branch.api;

import com.backend.branch.api.dto.BranchSummaryResponse;
import com.backend.branch.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
public class BranchPublicApi {

    private final BranchService branchService;

    public BranchPublicApi(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<List<BranchSummaryResponse>> listActive() {
        return ResponseEntity.ok(branchService.listActiveBranches());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchSummaryResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.getBranch(id));
    }
}
