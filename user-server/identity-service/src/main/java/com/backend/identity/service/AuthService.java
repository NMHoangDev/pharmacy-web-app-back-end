package com.backend.identity.service;

import com.backend.identity.api.dto.AdminUserIdentitySummary;
import com.backend.identity.api.dto.AuthResponse;
import com.backend.identity.api.dto.ChangePasswordRequest;
import com.backend.identity.api.dto.LoginRequest;
import com.backend.identity.api.dto.RegisterRequest;
import com.backend.identity.model.UserAccount;
import com.backend.identity.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final Keycloak keycloak;

    // TODO: nên inject RestTemplate bean có timeout, nhưng giữ như bạn để
   
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret:}")
    private String keycloakClientSecret;

    @Value("${services.user.uri:http://localhost:7016}")
    private String userServiceUrl;

    @Value("${services.pharmacist.uri:http://localhost:7025}")
    private String pharmacistServiceUrl;

    public AuthResponse register(RegisterRequest request) {
        UsersResource usersResource = keycloak.realm(realm).users();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.email());
        userRep.setEmail(request.email());

        // ✅ Đảm bảo đủ field required của User Profile (hay bị thiếu lastName)
        userRep.setFirstName(request.fullName());
        userRep.setLastName(request.fullName()); // hoặc split tên nếu muốn

        userRep.setEnabled(true);
        userRep.setEmailVerified(true);

        userRep.setAttributes(Collections.singletonMap("phone", Collections.singletonList(request.phone())));

        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.password());
        cred.setTemporary(false);
        userRep.setCredentials(Collections.singletonList(cred));

        // ✅ Đặt rỗng ngay từ đầu
        userRep.setRequiredActions(Collections.emptyList());

        Response response = usersResource.create(userRep);
        try {
            if (response.getStatus() != 201) {
                String error = response.readEntity(String.class);
                if (response.getStatus() == 409) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
                }
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to register user in Keycloak");
            }

            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // ✅ TRIỆT TIÊU requiredActions lần 2 (Keycloak có thể auto-add sau create)
            UserResource ur = usersResource.get(userId);
            UserRepresentation created = ur.toRepresentation();
            log.info("KC after create: requiredActions={}, emailVerified={}, enabled={}",
                    created.getRequiredActions(), created.isEmailVerified(), created.isEnabled());

            created.setRequiredActions(Collections.emptyList());
            created.setEnabled(true);
            created.setEmailVerified(true);
            ur.update(created);

            // assign role
            try {
                RoleRepresentation userRole = keycloak.realm(realm).roles().get("USER").toRepresentation();
                ur.roles().realmLevel().add(Collections.singletonList(userRole));
            } catch (Exception e) {
                log.error("Failed to assign USER role to Keycloak user {}: {}", userId, e.getMessage());
            }

            // local sync
            UUID userUuid = UUID.fromString(userId);
            repository.save(new UserAccount(
                    userUuid,
                    request.email(),
                    request.phone(),
                    "KEYCLOAK_MANAGED",
                    request.fullName(),
                    Collections.singleton("ROLE_USER")));

            syncWithUserService(userId, request);

                return new AuthResponse(userUuid, request.email(), request.phone(), request.fullName(), null, null, null, null);

        } catch (Exception e) {
            // rollback best-effort
            try {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                usersResource.get(userId).remove();
            } catch (Exception ignore) {
            }
            throw e instanceof ResponseStatusException
                    ? (ResponseStatusException) e
                    : new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Registration failed due to system error");
        } finally {
            response.close();
        }
    }

    private void syncWithUserService(String userId, RegisterRequest request) {
        try {
            if (userProfileExistsInUserService(userId)) {
                log.info("Skip syncing user-service for {} because profile already exists", userId);
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", userId);
            payload.put("email", request.email());
            payload.put("phone", request.phone());
            payload.put("fullName", request.fullName());

            restTemplate.put(userServiceUrl + "/api/users/" + userId, payload);
        } catch (Exception e) {
            log.warn("Failed to sync with user-service for {}: {}. This is non-blocking.", userId, e.getMessage());
        }
    }

    public void assignAdminManagedRole(UUID userId, String roleName) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }

        String targetRole = normalizeManagedRole(roleName);
        UserResource userResource = keycloak.realm(realm).users().get(userId.toString());
        UserRepresentation userRepresentation;
        try {
            userRepresentation = userResource.toRepresentation();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in Keycloak");
        }

        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
        List<String> currentManagedRoles = currentRoles.stream()
                .map(RoleRepresentation::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(this::isManagedRole)
                .distinct()
                .toList();

        List<String> nextRoles = new ArrayList<>();
        nextRoles.add("USER");
        if (!"USER".equals(targetRole)) {
            nextRoles.add(targetRole);
        }

        try {
            replaceManagedRealmRoles(userResource, currentManagedRoles, nextRoles);
            upsertLocalIdentityAccount(userRepresentation, userId, nextRoles);
            if ("PHARMACIST".equals(targetRole)) {
                syncPharmacistProfile(userId, userRepresentation);
            }
            log.info("Assigned managed role {} to user {}", targetRole, userId);
        } catch (ResponseStatusException ex) {
            restoreManagedRealmRoles(userResource, nextRoles, currentManagedRoles);
            throw ex;
        } catch (Exception ex) {
            restoreManagedRealmRoles(userResource, nextRoles, currentManagedRoles);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to assign role for user");
        }
    }

    public List<AdminUserIdentitySummary> listAdminUserIdentitySummaries() {
        List<AdminUserIdentitySummary> summaries = new ArrayList<>();
        int first = 0;
        int pageSize = 200;

        while (true) {
            List<UserRepresentation> users = keycloak.realm(realm).users().list(first, pageSize);
            if (users == null || users.isEmpty()) {
                break;
            }

            summaries.addAll(users.stream()
                    .map(this::toAdminUserIdentitySummary)
                    .filter(Objects::nonNull)
                    .toList());

            if (users.size() < pageSize) {
                break;
            }
            first += pageSize;
        }

        return summaries;
    }

    /**
     * Authenticates user via Keycloak token endpoint using password grant.
     */
    public AuthResponse login(LoginRequest request) {
        String tokenUrl = getTokenUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", keycloakClientId);

        if (keycloakClientSecret != null && !keycloakClientSecret.isBlank()) {
            body.add("client_secret", keycloakClientSecret);
        }

        body.add("username", request.identifier());
        body.add("password", request.password());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak returned empty response");
            }

            String accessToken = (String) responseBody.get("access_token");
            Number expiresInNum = (Number) responseBody.get("expires_in");
            long expiresIn = expiresInNum != null ? expiresInNum.longValue() : 0L;
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);
                String refreshToken = (String) responseBody.get("refresh_token");
                Number refreshExpiresInNum = (Number) responseBody.get("refresh_expires_in");
                long refreshExpiresIn = refreshExpiresInNum != null ? refreshExpiresInNum.longValue() : 0L;
                Instant refreshExpiresAt = refreshExpiresIn > 0 ? Instant.now().plusSeconds(refreshExpiresIn) : null;

            return new AuthResponse(
                    null,
                    request.identifier(),
                    null,
                    null,
                    accessToken,
                    expiresAt,
                    refreshToken,
                    refreshExpiresAt);

        } catch (HttpClientErrorException e) {
            String bodyErr = e.getResponseBodyAsString();
            log.error("Keycloak login failed with status {}: {}", e.getStatusCode(), bodyErr);

            // Bubble up clearer reason for "Account is not fully set up"
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST && bodyErr != null
                    && bodyErr.contains("not fully set up")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Account is not fully set up (required actions: verify email / update profile / update password / configure OTP)");
            }

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials or access denied");
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Authentication service error");
        } catch (Exception e) {
            log.error("Unexpected error during Keycloak login: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        String tokenUrl = getTokenUrl();
        String normalizedRefreshToken = refreshToken == null ? "" : refreshToken.trim();
        if (normalizedRefreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", keycloakClientId);
        if (keycloakClientSecret != null && !keycloakClientSecret.isBlank()) {
            body.add("client_secret", keycloakClientSecret);
        }
        body.add("refresh_token", normalizedRefreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak returned empty response");
            }

            String accessToken = (String) responseBody.get("access_token");
            Number expiresInNum = (Number) responseBody.get("expires_in");
            long expiresIn = expiresInNum != null ? expiresInNum.longValue() : 0L;
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);

            String nextRefreshToken = (String) responseBody.get("refresh_token");
            Number refreshExpiresInNum = (Number) responseBody.get("refresh_expires_in");
            long refreshExpiresIn = refreshExpiresInNum != null ? refreshExpiresInNum.longValue() : 0L;
            Instant refreshExpiresAt = refreshExpiresIn > 0 ? Instant.now().plusSeconds(refreshExpiresIn) : null;

            return new AuthResponse(
                    null,
                    null,
                    null,
                    null,
                    accessToken,
                    expiresAt,
                    nextRefreshToken,
                    refreshExpiresAt);
        } catch (HttpClientErrorException e) {
            log.warn("Refresh token request failed with status {}", e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Authentication provider rejected refresh request");
        } catch (HttpServerErrorException e) {
            log.error("Refresh token failed due to Keycloak server error {}", e.getStatusCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication provider is temporarily unavailable");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to refresh access token");
        }
    }

    public boolean checkKeycloakHealth() {
        try {
            // Note: /health/live depends on Keycloak management config; if 404, use openid
            // config instead
            String healthUrl = keycloakServerUrl.replaceAll("/$", "") + "/health/live";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Keycloak health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String getTokenUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl.replaceAll("/$", ""), realm);
    }

    /**
     * WARNING: This resets password as admin (no oldPassword check).
     * Ensure controller authorizes "self-change" OR implement old password
     * verification.
     */
    public void changePassword(ChangePasswordRequest request) {
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.newPassword());
        cred.setTemporary(false);

        keycloak.realm(realm).users().get(request.userId().toString()).resetPassword(cred);
        log.info("Changed password for user {}", request.userId());
    }

    public AuthResponse getUserInfoFromToken(org.springframework.security.oauth2.jwt.Jwt jwt) {
        return new AuthResponse(
                safeParseUuid(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                null, // Phone not usually in standard JWT claims unless mapped
                jwt.getClaimAsString("name"),
                jwt.getTokenValue(),
                jwt.getExpiresAt(),
                null,
                null);
    }

    public AuthResponse syncSocialUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user");
        }

        UUID userId = safeParseUuid(jwt.getSubject());
        ensureRealmRole(userId, "USER");

        String email = trimToNull(jwt.getClaimAsString("email"));
        String fullName = firstNonBlank(
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("preferred_username"),
                email,
                userId.toString());
        String phone = firstNonBlank(
                jwt.getClaimAsString("phone_number"),
                readFirstAttribute(jwt, "phone"),
                "");

        Optional<UserAccount> existingAccount = repository.findById(userId)
                .or(() -> email == null ? Optional.empty() : repository.findByEmail(email));

        UserAccount saved = existingAccount.orElseGet(() -> {
            UserAccount account = new UserAccount(
                    userId,
                    email,
                    phone,
                    "KEYCLOAK_MANAGED",
                    fullName,
                    Collections.singleton("ROLE_USER"));
            UserAccount created = repository.save(account);
            syncWithUserService(
                    created.id().toString(),
                    new RegisterRequest(
                            safeValue(created.fullName(), fullName),
                            safeValue(created.email(), email),
                            safeValue(created.phone(), phone),
                            "KEYCLOAK_MANAGED"));
            return created;
        });

        return new AuthResponse(
                saved.id(),
                saved.email(),
                saved.phone(),
                saved.fullName(),
                jwt.getTokenValue(),
                jwt.getExpiresAt(),
                null,
                null);
    }

    private UUID safeParseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Keycloak user id is not a valid UUID: " + id);
        }
    }

    private String safeReadEntity(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            return "<unreadable response body>";
        }
    }

    @SuppressWarnings("unchecked")
    private String readFirstAttribute(Jwt jwt, String key) {
        Object raw = jwt.getClaims().get(key);
        if (raw instanceof String str && !str.isBlank()) {
            return str;
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(v -> !v.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        Map<String, Object> claims = jwt.getClaims();
        Object attributesRaw = claims.get("attributes");
        if (attributesRaw instanceof Map<?, ?> attributes) {
            Object value = attributes.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .filter(v -> !v.isBlank())
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return "";
    }

    private boolean userProfileExistsInUserService(String userId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userServiceUrl + "/api/users/" + userId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null || body.isEmpty()) {
                return false;
            }

            String email = trimToNull(String.valueOf(body.getOrDefault("email", "")));
            String fullName = trimToNull(String.valueOf(body.getOrDefault("fullName", "")));
            String phone = trimToNull(String.valueOf(body.getOrDefault("phone", "")));
            return email != null || fullName != null || phone != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private String safeValue(String primary, String fallback) {
        String trimmedPrimary = trimToNull(primary);
        if (trimmedPrimary != null) {
            return trimmedPrimary;
        }
        String trimmedFallback = trimToNull(fallback);
        return trimmedFallback != null ? trimmedFallback : "";
    }

    private void ensureRealmRole(UUID userId, String roleName) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(userId.toString());
            List<RoleRepresentation> existingRoles = userResource.roles().realmLevel().listAll();
            boolean hasRole = existingRoles.stream()
                    .map(RoleRepresentation::getName)
                    .filter(Objects::nonNull)
                    .anyMatch(roleName::equalsIgnoreCase);
            if (hasRole) {
                return;
            }

            RoleRepresentation targetRole = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(targetRole));
            log.info("Assigned realm role {} to social user {}", roleName, userId);
        } catch (Exception ex) {
            log.warn("Failed to ensure role {} for social user {}: {}", roleName, userId, ex.getMessage());
        }
    }

    private AdminUserIdentitySummary toAdminUserIdentitySummary(UserRepresentation userRepresentation) {
        if (userRepresentation == null || userRepresentation.getId() == null || userRepresentation.getId().isBlank()) {
            return null;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userRepresentation.getId());
        } catch (IllegalArgumentException ex) {
            log.debug("Skip Keycloak user with non-UUID id {}", userRepresentation.getId());
            return null;
        }

        List<String> keycloakRoles = keycloak.realm(realm).users().get(userRepresentation.getId())
                .roles()
                .realmLevel()
                .listAll()
                .stream()
                .map(RoleRepresentation::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        boolean enabled = userRepresentation.isEnabled();
        boolean emailVerified = userRepresentation.isEmailVerified();

        return new AdminUserIdentitySummary(
                userId,
                resolveAdminRole(keycloakRoles),
                keycloakRoles,
                resolveAdminStatus(userRepresentation),
                enabled,
                emailVerified);
    }

    private String resolveAdminRole(List<String> keycloakRoles) {
        Set<String> normalizedRoles = keycloakRoles == null ? Set.of() : keycloakRoles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        if (normalizedRoles.contains("ADMIN")) {
            return "admin";
        }
        if (normalizedRoles.contains("PHARMACIST")) {
            return "pharmacist";
        }
        return "customer";
    }

    private String resolveAdminStatus(UserRepresentation userRepresentation) {
        if (userRepresentation == null) {
            return "pending";
        }
        if (!userRepresentation.isEnabled()) {
            return "suspended";
        }
        List<String> requiredActions = userRepresentation.getRequiredActions();
        if (!userRepresentation.isEmailVerified()
                || (requiredActions != null && !requiredActions.isEmpty())) {
            return "pending";
        }
        return "active";
    }

    private String normalizeManagedRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        return switch (normalized) {
            case "USER", "PHARMACIST", "ADMIN" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
        };
    }

    private boolean isManagedRole(String roleName) {
        return "USER".equals(roleName) || "PHARMACIST".equals(roleName) || "ADMIN".equals(roleName);
    }

    private void replaceManagedRealmRoles(UserResource userResource, List<String> currentRoles, List<String> nextRoles) {
        List<RoleRepresentation> rolesToRemove = currentRoles == null ? List.of() : currentRoles.stream()
                .filter(currentRole -> nextRoles == null || nextRoles.stream().noneMatch(currentRole::equalsIgnoreCase))
                .map(role -> keycloak.realm(realm).roles().get(role).toRepresentation())
                .toList();
        if (!rolesToRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(rolesToRemove);
        }

        List<RoleRepresentation> rolesToAdd = nextRoles == null ? List.of() : nextRoles.stream()
                .filter(nextRole -> currentRoles == null || currentRoles.stream().noneMatch(nextRole::equalsIgnoreCase))
                .map(role -> keycloak.realm(realm).roles().get(role).toRepresentation())
                .toList();
        if (!rolesToAdd.isEmpty()) {
            userResource.roles().realmLevel().add(rolesToAdd);
        }
    }

    private void restoreManagedRealmRoles(UserResource userResource, List<String> attemptedRoles, List<String> previousRoles) {
        try {
            replaceManagedRealmRoles(userResource, attemptedRoles, previousRoles);
        } catch (Exception rollbackError) {
            log.warn("Failed to rollback managed roles for {}: {}", userResource.toRepresentation().getId(),
                    rollbackError.getMessage());
        }
    }

    private void upsertLocalIdentityAccount(UserRepresentation userRepresentation, UUID userId, List<String> roles) {
        Set<String> localRoles = roles == null ? Set.of("ROLE_USER") : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .map(value -> "ROLE_" + value)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        String email = trimToNull(userRepresentation.getEmail());
        String phone = trimToNull(extractPhone(userRepresentation));
        String fullName = firstNonBlank(
                userRepresentation.getFirstName(),
                userRepresentation.getLastName(),
                userRepresentation.getUsername(),
                email,
                userId.toString());

        repository.upsert(new UserAccount(
                userId,
                email,
                phone,
                "KEYCLOAK_MANAGED",
                fullName,
                localRoles));
    }

    private void syncPharmacistProfile(UUID userId, UserRepresentation userRepresentation) {
        Map<String, Object> profile = fetchUserProfile(userId);
        String email = pickFirstNonBlank(
                safeMapValue(profile, "email"),
                trimToNull(userRepresentation.getEmail()),
                trimToNull(userRepresentation.getUsername()));
        String phone = pickFirstNonBlank(
                safeMapValue(profile, "phone"),
                trimToNull(extractPhone(userRepresentation)));
        String fullName = pickFirstNonBlank(
                safeMapValue(profile, "fullName"),
                trimToNull(userRepresentation.getFirstName()),
                trimToNull(userRepresentation.getLastName()),
                trimToNull(userRepresentation.getUsername()),
                email,
                userId.toString());

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        payload.put("email", email);
        payload.put("phone", phone);
        payload.put("avatarUrl", null);
        payload.put("specialty", "general");

        try {
            restTemplate.put(pharmacistServiceUrl + "/internal/pharmacists/" + userId + "/bootstrap", payload);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to sync pharmacist profile");
        }
    }

    private Map<String, Object> fetchUserProfile(UUID userId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userServiceUrl + "/api/users/" + userId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return response.getBody() == null ? Map.of() : response.getBody();
        } catch (RestClientException ex) {
            return Map.of();
        }
    }

    private String safeMapValue(Map<String, Object> payload, String key) {
        if (payload == null || payload.isEmpty() || key == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String pickFirstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String extractPhone(UserRepresentation userRepresentation) {
        if (userRepresentation == null || userRepresentation.getAttributes() == null) {
            return null;
        }
        List<String> phones = userRepresentation.getAttributes().get("phone");
        if (phones == null || phones.isEmpty()) {
            return null;
        }
        return trimToNull(phones.get(0));
    }
}
