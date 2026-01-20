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
            @Value("${identity.jwt.secret}") String secret,
            @Value("${identity.jwt.expires-minutes:60}") long expirationMinutes,
            @Value("${identity.jwt.issuer:http://localhost:7015}") String issuer) {
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