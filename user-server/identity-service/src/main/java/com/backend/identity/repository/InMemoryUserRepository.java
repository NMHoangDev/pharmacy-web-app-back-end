package com.backend.identity.repository;

import com.backend.identity.model.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Repository
@ConditionalOnProperty(prefix = "spring.datasource", name = "url", matchIfMissing = true, havingValue = "")
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, UserAccount> usersByPhone = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(normalize(email)));
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone) {
        return Optional.ofNullable(usersByPhone.get(normalize(phone)));
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return usersByEmail.values().stream().filter(u -> u.id().equals(id)).findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(normalize(email));
    }

    @Override
    public boolean existsByPhone(String phone) {
        return usersByPhone.containsKey(normalize(phone));
    }

    @Override
    public UserAccount save(UserAccount account) {
        if (account.email() != null && !account.email().isBlank()) {
            usersByEmail.put(normalize(account.email()), account);
        }
        if (account.phone() != null && !account.phone().isBlank()) {
            usersByPhone.put(normalize(account.phone()), account);
        }

        return account;
    }

    @Override
    public void updatePassword(UUID id, String passwordHash) {
        usersByEmail.replaceAll((k, v) -> v.id().equals(id)
                ? new UserAccount(v.id(), v.email(), v.phone(), passwordHash, v.fullName(), v.roles())
                : v);
        usersByPhone.replaceAll((k, v) -> v.id().equals(id)
                ? new UserAccount(v.id(), v.email(), v.phone(), passwordHash, v.fullName(), v.roles())
                : v);
    }

    private String normalize(String v) {
        return v == null ? "" : v.trim().toLowerCase();
    }
}