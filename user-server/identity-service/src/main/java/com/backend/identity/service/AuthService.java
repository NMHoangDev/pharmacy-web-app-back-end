package com.backend.identity.service;

import com.backend.identity.api.dto.AuthResponse;
import com.backend.identity.api.dto.ChangePasswordRequest;
import com.backend.identity.api.dto.LoginRequest;
import com.backend.identity.api.dto.RegisterRequest;
import com.backend.identity.model.UserAccount;
import com.backend.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public AuthService(UserRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RestTemplateBuilder restTemplateBuilder,
            @org.springframework.beans.factory.annotation.Value("${services.user.uri:http://localhost:7016}") String userServiceUrl) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.restTemplate = restTemplateBuilder.build();
        this.userServiceUrl = userServiceUrl;
    }

    public AuthResponse register(RegisterRequest request) {
        boolean emailExists = repository.existsByEmail(request.email());
        boolean phoneExists = repository.existsByPhone(request.phone());
        if (emailExists || phoneExists) {
            if (emailExists && phoneExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email and phone already registered");
            } else if (emailExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
            }
        }

        UserAccount user = new UserAccount(
                UUID.randomUUID(),
                request.email().trim().toLowerCase(),
                request.phone().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                Set.of("ROLE_USER"));

        repository.save(user);

        // Try to create profile in user-service with same UUID
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("email", user.email());
            payload.put("phone", user.phone());
            payload.put("fullName", user.fullName());
            payload.put("avatarBase64", null);
            // Ensure id is set by passing same id in payload if user-service supports it
            payload.put("id", user.id().toString());

            String url = userServiceUrl + "/api/users/" + user.id().toString();
            log.info("Upserting user profile in user-service for id={} url={}", user.id(), url);
            // Use PUT to upsert
            restTemplate.put(url, payload);
            log.info("user-service upsert PUT completed for id={}", user.id());
        } catch (RestClientException ex) {
            // Do not fail registration if user-service unavailable; log for investigation
            String respBody = "";
            if (ex instanceof org.springframework.web.client.HttpStatusCodeException hsc) {
                respBody = hsc.getResponseBodyAsString();
            }
            log.warn("Failed to create profile in user-service for id={} : {} {}", user.id(), ex.toString(), respBody);
        }

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String id = request.identifier().trim();
        var byEmail = repository.findByEmail(id);
        var byPhone = repository.findByPhone(id);
        var found = byEmail.or(() -> byPhone);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }

        UserAccount user = found.get();

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }

        return buildResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        UserAccount user = repository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
        }

        String newHash = passwordEncoder.encode(request.newPassword());
        repository.updatePassword(user.id(), newHash);
    }

    private AuthResponse buildResponse(UserAccount user) {
        JwtService.JwtResult token = jwtService.generateToken(user);
        return new AuthResponse(user.id(), user.email(), user.phone(), user.fullName(), token.token(),
                token.expiresAt());
    }
}