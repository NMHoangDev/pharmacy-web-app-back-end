package com.backend.settings.repository;

import com.backend.settings.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByScopeAndKey(String scope, String key);

    List<Setting> findByScope(String scope);
}
