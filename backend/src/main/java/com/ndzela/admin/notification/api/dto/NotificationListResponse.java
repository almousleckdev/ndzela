package com.ndzela.admin.notification.api.dto;

import java.util.List;

public record NotificationListResponse(long unreadCount, List<NotificationResponse> items) {}
