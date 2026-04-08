package com.backend.identity.model;

import java.util.Set;
import java.util.UUID;

public record UserAccount(UUID id, String email, String phone, String passwordHash, String fullName,
        Set<String> roles) {
}