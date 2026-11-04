package com.hpote.backend.community.model;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.user.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service_request_likes", uniqueConstraints = @UniqueConstraint(
        name = "uk_service_request_like_user", columnNames = {"service_request_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestLike extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
