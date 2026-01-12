package com.backend.common.security;

/**
 * Shared JWT claim names across services.
 */
public final class JwtClaims {
    private JwtClaims() {
    }

    public static final String SUBJECT = "sub";
    public static final String ROLES = "roles";
    public static final String SCOPE = "scope";
    public static final String TENANT = "tenant";
    public static final String EMAIL = "email";
    public static final String NAME = "name";
}
