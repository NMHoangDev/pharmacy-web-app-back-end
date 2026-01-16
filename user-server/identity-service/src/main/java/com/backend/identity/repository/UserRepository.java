package com.backend.identity.repository;

import com.backend.identity.model.UserAccount;

import java.util.Optional;

public interface UserRepository {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    UserAccount save(UserAccount account);
}
