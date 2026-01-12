package com.backend.settings.service;

import com.backend.settings.api.dto.SettingRequest;
import com.backend.settings.model.Setting;
import com.backend.settings.repository.SettingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingService {

    private final SettingRepository repository;

    public List<Setting> list(String scope) {
        return scope == null ? repository.findAll() : repository.findByScope(scope);
    }

    public Setting get(Long id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Setting not found"));
    }

    public Setting upsert(SettingRequest request) {
        Setting setting = repository.findByScopeAndKey(request.scope(), request.key()).orElse(new Setting());
        setting.setScope(request.scope());
        setting.setKey(request.key());
        setting.setValue(request.value());
        setting.setDescription(request.description());
        setting.setSecure(request.secure() != null && request.secure());
        return repository.save(setting);
    }

    public Setting update(Long id, SettingRequest request) {
        Setting setting = get(id);
        setting.setScope(request.scope());
        setting.setKey(request.key());
        setting.setValue(request.value());
        setting.setDescription(request.description());
        setting.setSecure(request.secure() != null && request.secure());
        return repository.save(setting);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
