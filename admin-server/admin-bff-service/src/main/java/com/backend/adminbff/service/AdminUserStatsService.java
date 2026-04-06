package com.backend.adminbff.service;

import com.backend.adminbff.dto.AdminUserProfile;
import com.backend.adminbff.dto.stats.UsersStatsResponse;
import com.backend.adminbff.repository.AdminUserStatsRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class AdminUserStatsService {

    private final AdminUserStatsRepository repository;

    public AdminUserStatsService(AdminUserStatsRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = "admin-users-stats", key = "#range")
    public UsersStatsResponse getStats(String range) {
        List<AdminUserProfile> users = repository.findAllUsers();
        long total = users.size();

        // Current upstream user API does not expose status/approval timestamps yet.
        // Keep fields stable for frontend cards and backfill when user-service adds these
        // columns.
        return new UsersStatsResponse(
                total,
                total,
                0,
                0,
                0,
                null);
    }
}
