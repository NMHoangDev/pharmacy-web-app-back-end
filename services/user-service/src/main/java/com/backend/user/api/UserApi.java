package com.backend.user.api;

import com.backend.user.api.dto.AddressRequest;
import com.backend.user.api.dto.AddressResponse;
import com.backend.user.api.dto.HealthProfileRequest;
import com.backend.user.api.dto.HealthProfileResponse;
import com.backend.user.api.dto.ProfileRequest;
import com.backend.user.api.dto.ProfileResponse;
import com.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserApi {

    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserApi.class);

    public UserApi(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("user-service ok");
    }

    // Profile
    @PostMapping
    public ResponseEntity<ProfileResponse> create(@RequestBody @Valid ProfileRequest req) {
        return ResponseEntity.ok(userService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<ProfileResponse>> list() {
        return ResponseEntity.ok(userService.list());
    }

    // @GetMapping("/stats")
    // public ResponseEntity<Map<String, Object>> stats(@RequestParam(defaultValue = "7d") String range) {
    //     long total = userService.list().size();
    //     Map<String, Object> metrics = Map.of(
    //             "totalUsers", total,
    //             "activeUsers", total,
    //             "pendingApprovalUsers", 0,
    //             "blockedUsers", 0,
    //             "newUsersLast7Days", 0,
    //             "weekOverWeekGrowthPct", 0);
    //     return ResponseEntity.ok(Map.of(
    //             "metrics", metrics,
    //             "generatedAt", Instant.now().toString(),
    //             "range", range));
    // }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> get(@PathVariable UUID id) {
        try {
            ProfileResponse profile = userService.get(id);
            return ResponseEntity.ok(profile);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            // If user not found, return a minimal profile instead of 404 so downstream
            // flows (change-password, order history)
            // can proceed without failing when profile is missing.
            log.warn("UserApi.get not found id={}; returning minimal profile to avoid breaking consumers", id);
            if (rse.getStatusCode().value() == 404) {
                return ResponseEntity.ok(new ProfileResponse(id, "", "", "", null, null));
            }
            return ResponseEntity.status(rse.getStatusCode()).body(null);
        } catch (Exception ex) {
            log.error("Unexpected error fetching user id={}: {}", id, ex.toString(), ex);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> update(@PathVariable UUID id, @RequestBody @Valid ProfileRequest req) {
        log.info("Received PUT /api/users/{} payload={}", id, req);
        try {
            ProfileResponse resp = userService.update(id, req);
            return ResponseEntity.ok(resp);
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            log.warn("UserApi.update returns {} for id={}", rse.getStatusCode(), id);
            return ResponseEntity.status(rse.getStatusCode()).body(null);
        } catch (Exception ex) {
            log.error("Error updating user id={}: {}", id, ex.toString(), ex);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Addresses
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.listAddresses(userId));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(@PathVariable UUID userId,
            @RequestBody @Valid AddressRequest req) {
        return ResponseEntity.ok(userService.addAddress(userId, req));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable UUID userId,
            @PathVariable UUID addressId,
            @RequestBody @Valid AddressRequest req) {
        return ResponseEntity.ok(userService.updateAddress(userId, addressId, req));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID userId, @PathVariable UUID addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    // Health profile (lite)
    @GetMapping("/{userId}/health")
    public ResponseEntity<HealthProfileResponse> getHealth(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getHealth(userId));
    }

    @PutMapping("/{userId}/health")
    public ResponseEntity<HealthProfileResponse> upsertHealth(@PathVariable UUID userId,
            @RequestBody @Valid HealthProfileRequest req) {
        return ResponseEntity.ok(userService.upsertHealth(userId, req));
    }
}
