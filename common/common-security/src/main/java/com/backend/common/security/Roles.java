package com.backend.common.security;

/**
 * Role constants for RBAC.
 */
public final class Roles {
    private Roles() {
    }

    public static final String ADMIN = "ROLE_ADMIN";
    public static final String PHARMACIST = "ROLE_PHARMACIST";
    public static final String CUSTOMER_SERVICE = "ROLE_CS";
    public static final String WAREHOUSE = "ROLE_WAREHOUSE";
    public static final String USER = "ROLE_USER";
}
