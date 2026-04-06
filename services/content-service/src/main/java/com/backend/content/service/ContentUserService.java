package com.backend.content.service;

import com.backend.content.api.dto.UserSummaryDto;
import com.backend.content.model.ContentUser;
import com.backend.content.repo.ContentUserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentUserService {

    private final ContentUserRepository userRepository;

    public ContentUserService(ContentUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ContentUser ensureUser(UUID id, String displayName, String role) {
        if (id == null) {
            return null;
        }
        ContentUser user = userRepository.findById(id).orElseGet(ContentUser::new);
        if (user.getId() == null) {
            user.setId(id);
            user.setCreatedAt(Instant.now());
        }
        if (displayName != null && !displayName.isBlank()) {
            user.setDisplayName(displayName.trim());
        } else if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName("Người dùng");
        }
        if (role != null && !role.isBlank()) {
            user.setRole(role.toUpperCase());
        } else if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }
        return userRepository.save(user);
    }

    public Map<UUID, ContentUser> findByIds(List<UUID> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ContentUser::getId, Function.identity()));
    }

    public UserSummaryDto toSummary(ContentUser user, boolean isAnonymous) {
        if (user == null) {
            return new UserSummaryDto(null, isAnonymous ? "Ẩn danh" : "Người dùng", isAnonymous, null);
        }
        String name = isAnonymous ? "Ẩn danh" : user.getDisplayName();
        return new UserSummaryDto(user.getId(), name, isAnonymous, user.getRole());
    }
}
