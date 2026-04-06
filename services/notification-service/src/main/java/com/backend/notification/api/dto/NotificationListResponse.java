package com.backend.notification.api.dto;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> items, long unreadCount) {
}