package com.hpote.backend.messaging.model;

import java.time.LocalDateTime;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_messages", indexes = {
        @Index(name = "idx_service_messages_request_created", columnList = "service_request_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceMessage extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime readAt;
}
