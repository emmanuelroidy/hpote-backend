package com.hpote.backend.messaging.dto;

import java.time.LocalDateTime;
import com.hpote.backend.messaging.model.ServiceMessage;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceMessageView {
    String id;
    String serviceRequestId;
    String senderId;
    String senderName;
    String content;
    LocalDateTime createdAt;
    LocalDateTime readAt;

    public static ServiceMessageView from(ServiceMessage message) {
        return ServiceMessageView.builder()
                .id(message.getId())
                .serviceRequestId(message.getServiceRequest().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }
}
