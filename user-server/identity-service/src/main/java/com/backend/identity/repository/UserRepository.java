package com.backend.identity.repository;

import com.backend.identity.model.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByPhone(String phone);

    Optional<UserAccount> findById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    UserAccount save(UserAccount account);

    void updatePassword(UUID id, String passwordHash);
}
