package com.hpote.backend.community.model;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.user.model.User;

import jakarta.persistence.Column;
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
@Table(name = "professional_recommendations", uniqueConstraints = @UniqueConstraint(
        name = "uk_recommendation_request_user_professional",
        columnNames = {"service_request_id", "recommender_id", "professional_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalRecommendation extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recommender_id", nullable = false)
    private User recommender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private User professional;

    @Column(length = 240)
    private String note;
}
