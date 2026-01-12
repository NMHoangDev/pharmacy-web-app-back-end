package com.backend.settings.api;

import com.backend.settings.api.dto.SettingRequest;
import com.backend.settings.model.Setting;
import com.backend.settings.service.SettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingService settingService;

    @GetMapping
    public List<Setting> list(@RequestParam(required = false) String scope) {
        return settingService.list(scope);
    }

    @GetMapping("/{id}")
    public Setting get(@PathVariable Long id) {
        return settingService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Setting upsert(@Valid @RequestBody SettingRequest request) {
        return settingService.upsert(request);
    }

    @PutMapping("/{id}")
    public Setting update(@PathVariable Long id, @Valid @RequestBody SettingRequest request) {
        return settingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        settingService.delete(id);
    }
}
