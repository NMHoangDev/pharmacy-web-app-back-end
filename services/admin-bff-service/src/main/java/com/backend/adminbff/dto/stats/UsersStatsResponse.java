package com.backend.adminbff.dto.stats;

public record UsersStatsResponse(
        long totalUsers,
        long activeUsers,
        long pendingApprovalUsers,
        long blockedUsers,
        long newUsersLast7Days,
        Double weekOverWeekGrowthPct) {
}
