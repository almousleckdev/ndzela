package com.ndzela.admin.notification.api;

import com.ndzela.admin.notification.api.dto.NotificationListResponse;
import com.ndzela.admin.notification.api.dto.NotificationResponse;
import com.ndzela.admin.notification.application.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse list(Authentication authentication) {
        return notificationService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public NotificationResponse get(Authentication authentication, @PathVariable UUID id) {
        return notificationService.get(authentication.getName(), id);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(Authentication authentication, @PathVariable UUID id) {
        notificationService.markRead(authentication.getName(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        notificationService.markAllRead(authentication.getName());
    }
}
