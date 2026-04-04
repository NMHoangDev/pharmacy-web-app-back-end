package com.backend.identity.api;

import com.backend.identity.api.dto.AuthResponse;
import com.backend.identity.api.dto.AdminUserIdentitySummary;
import com.backend.identity.api.dto.ChangePasswordRequest;
import com.backend.identity.api.dto.LoginRequest;
import com.backend.identity.api.dto.RefreshTokenRequest;
import com.backend.identity.api.dto.RegisterRequest;
import com.backend.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("identity-service ok");
    }

    @GetMapping("/health/keycloak")
    public ResponseEntity<java.util.Map<String, Object>> checkKeycloak() {
        boolean isUp = authService.checkKeycloakHealth();
        return ResponseEntity.ok(java.util.Map.of(
                "status", isUp ? "UP" : "DOWN",
                "service", "keycloak"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return ResponseEntity.ok(authService.getUserInfoFromToken(jwt));
    }

    @GetMapping("/admin/users/identity")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserIdentitySummary>> listAdminUserIdentity() {
        return ResponseEntity.ok(authService.listAdminUserIdentitySummaries());
    }

    @PostMapping("/social/sync")
    public ResponseEntity<AuthResponse> syncSocialUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return ResponseEntity.ok(authService.syncSocialUser(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}/role")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateRole(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID userId,
            @org.springframework.web.bind.annotation.RequestParam String role) {
        authService.updateRole(userId, role);
        return ResponseEntity.ok().build();
    }
}
