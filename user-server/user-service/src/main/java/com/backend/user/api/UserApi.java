package com.backend.user.api;

import com.backend.user.api.dto.AddressRequest;
import com.backend.user.api.dto.AddressResponse;
import com.backend.user.api.dto.HealthProfileRequest;
import com.backend.user.api.dto.HealthProfileResponse;
import com.backend.user.api.dto.ProfileRequest;
import com.backend.user.api.dto.ProfileResponse;
import com.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserApi {

    private final UserService userService;

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

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfileResponse> update(@PathVariable UUID id, @RequestBody @Valid ProfileRequest req) {
        return ResponseEntity.ok(userService.update(id, req));
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
