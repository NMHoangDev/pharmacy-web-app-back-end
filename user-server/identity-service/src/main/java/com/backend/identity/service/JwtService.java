package com.backend.identity.service;

import com.backend.identity.model.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(
            @Value("${identity.jwt.secret:${IDENTITY_JWT_SECRET:dev-secret-change-me}}") String secret,
            @Value("${identity.jwt.expires-minutes:${IDENTITY_JWT_EXPIRES:60}}") long expirationMinutes,
            @Value("${identity.jwt.issuer:${IDENTITY_JWT_ISSUER:http://localhost:7070}}") String issuer) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT secret cannot be empty. Please set IDENTITY_JWT_SECRET environment variable.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    public JwtResult generateToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.email());
        claims.put("name", user.fullName());
        claims.put("roles", user.roles());

        String token = Jwts.builder()
                .setIssuer(issuer)
                .setSubject(user.id().toString())
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();

        return new JwtResult(token, expiry);
    }

    public record JwtResult(String token, Instant expiresAt) {
    }
}