package com.backend.adminbff.repository;

import com.backend.adminbff.dto.AdminUserProfile;
import java.util.List;
import org.springframework.stereotype.Repository;
import com.backend.adminbff.client.AdminUserClient;

@Repository
public class AdminUserStatsRepository {

    private final AdminUserClient adminUserClient;

    public AdminUserStatsRepository(AdminUserClient adminUserClient) {
        this.adminUserClient = adminUserClient;
    }

    public List<AdminUserProfile> findAllUsers() {
        return adminUserClient.listUsers();
    }
}
