package com.hpote.backend.notification.dto;

import java.time.LocalDateTime;
import com.hpote.backend.notification.model.AppNotification;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppNotificationView {
    String id;
    String type;
    String title;
    String body;
    String resourceId;
    LocalDateTime createdAt;
    LocalDateTime readAt;

    public static AppNotificationView from(AppNotification notification) {
        return AppNotificationView.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .resourceId(notification.getResourceId())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
