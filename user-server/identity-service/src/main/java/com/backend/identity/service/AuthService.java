package com.backend.identity.service;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository repository;
    private final Keycloak keycloak;

    // TODO: nên inject RestTemplate bean có timeout, nhưng giữ như bạn để
    // copy-paste chạy ngay
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

            return new AuthResponse(userUuid, request.email(), request.phone(), request.fullName(), null, null);

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

    public void updateRole(UUID userId, String roleName) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserResource userResource = usersResource.get(userId.toString());

        RoleRepresentation roleRep = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        userResource.roles().realmLevel().add(Collections.singletonList(roleRep));
        log.info("Assigned role {} to user {}", roleName, userId);
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

            return new AuthResponse(
                    null,
                    request.identifier(),
                    null,
                    null,
                    accessToken,
                    expiresAt);

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
                jwt.getExpiresAt());
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
}
