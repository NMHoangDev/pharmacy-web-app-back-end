package com.backend.identity.service;

import com.backend.identity.api.dto.AuthResponse;
import com.backend.identity.api.dto.LoginRequest;
import com.backend.identity.api.dto.RegisterRequest;
import com.backend.identity.model.UserAccount;
import com.backend.identity.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    private AuthResponse buildResponse(UserAccount user) {
        JwtService.JwtResult token = jwtService.generateToken(user);
        return new AuthResponse(user.id(), user.email(), user.phone(), user.fullName(), token.token(),
                token.expiresAt());
    }
}