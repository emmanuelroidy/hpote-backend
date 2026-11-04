package com.hpote.backend.notification.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.notification.dto.AppNotificationView;
import com.hpote.backend.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<AppNotificationView>>> getMine(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Notifications retrieved",
                notificationService.getMine(principal.getName())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseDto<Long>> getUnreadCount(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Unread notification count",
                notificationService.getUnreadCount(principal.getName())));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponseDto<AppNotificationView>> markRead(
            Principal principal, @PathVariable String notificationId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Notification marked as read",
                notificationService.markRead(principal.getName(), notificationId)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponseDto<Void>> markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Notifications marked as read", null));
    }
}
