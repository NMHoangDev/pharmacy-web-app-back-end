package com.backend.identity.repository;

import com.backend.identity.model.UserAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Primary
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class JdbcUserRepository implements UserRepository {

    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        var list = jdbc.query("SELECT id, email, phone, password_hash, full_name FROM users WHERE email = ?",
                rowMapper(), email.trim().toLowerCase());
        return list.stream().findFirst();
    }

    @Override
    public Optional<UserAccount> findByPhone(String phone) {
        var list = jdbc.query("SELECT id, email, phone, password_hash, full_name FROM users WHERE phone = ?",
                rowMapper(), phone.trim());
        return list.stream().findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        Integer cnt = jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE email = ?", Integer.class,
                email.trim().toLowerCase());
        return cnt != null && cnt > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        Integer cnt = jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE phone = ?", Integer.class, phone.trim());
        return cnt != null && cnt > 0;
    }

    @Override
    public UserAccount save(UserAccount account) {
        // insert into users (id,email,phone,password_hash,full_name) values (?,?,?,?,?)
        jdbc.update("INSERT INTO users (id, email, phone, password_hash, full_name) VALUES (?,?,?,?,?)",
                account.id().toString(), account.email(), account.phone(), account.passwordHash(), account.fullName());
        return account;
    }

    private RowMapper<UserAccount> rowMapper() {
        return new RowMapper<>() {
            @Override
            public UserAccount mapRow(ResultSet rs, int rowNum) throws SQLException {
                UUID id = UUID.fromString(rs.getString("id"));
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String passwordHash = rs.getString("password_hash");
                String fullName = rs.getString("full_name");
                return new UserAccount(id, email, phone, passwordHash, fullName, Set.of("ROLE_USER"));
            }
        };
    }
}
