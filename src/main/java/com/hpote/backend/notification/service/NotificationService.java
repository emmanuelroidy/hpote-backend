package com.hpote.backend.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.notification.dto.AppNotificationView;
import com.hpote.backend.notification.model.AppNotification;
import com.hpote.backend.notification.repository.AppNotificationRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void create(User recipient, String type, String title, String body, String resourceId) {
        notificationRepository.save(AppNotification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .resourceId(resourceId)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AppNotificationView> getMine(String username) {
        User user = findUser(username);
        return notificationRepository.findTop50ByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(AppNotificationView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(findUser(username).getId());
    }

    @Transactional
    public AppNotificationView markRead(String username, String notificationId) {
        User user = findUser(username);
        AppNotification notification = notificationRepository.findById(notificationId)
                .filter(item -> item.getRecipient().getId().equals(user.getId()))
                .orElseThrow(() -> new ApiExceptionDto("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return AppNotificationView.from(notification);
    }

    @Transactional
    public void markAllRead(String username) {
        User user = findUser(username);
        LocalDateTime readAt = LocalDateTime.now();
        notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId())
                .forEach(item -> item.setReadAt(readAt));
    }

    private User findUser(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
    }
}
