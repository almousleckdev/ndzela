package com.ndzela.admin.notification.application;

import com.ndzela.admin.common.exception.ResourceNotFoundException;
import com.ndzela.admin.notification.api.dto.NotificationListResponse;
import com.ndzela.admin.notification.api.dto.NotificationResponse;
import com.ndzela.admin.notification.domain.Notification;
import com.ndzela.admin.notification.domain.NotificationRepository;
import com.ndzela.admin.user.domain.User;
import com.ndzela.admin.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// In-app notifications
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository userRepository;

    @Transactional
    public void notify(User user, String type, String title, String message) {
        notifications.save(Notification.builder().userId(user.getId()).type(type).title(title).message(message).build());
    }

    @Transactional
    public void notifyHolders(String permission, UUID countryId, String excludeEmail, String type, String title, String message) {
        userRepository.findActiveByPermission(permission, countryId).stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase(excludeEmail))
                .forEach(user -> notify(user, type, title, message));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(String email) {
        UUID userId = userId(email);
        var items = notifications.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
        return new NotificationListResponse(notifications.countByUserIdAndReadFalse(userId), items);
    }

    @Transactional
    public void markRead(String email, UUID id) {
        notifications.markRead(userId(email), id);
    }

    @Transactional
    public void markAllRead(String email) {
        notifications.markAllRead(userId(email));
    }

    private UUID userId(String email) {
        return userRepository.findByEmail(email).map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
