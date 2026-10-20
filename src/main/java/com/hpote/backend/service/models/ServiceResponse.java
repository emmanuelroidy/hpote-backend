package com.hpote.backend.service.models;

import java.math.BigDecimal;

import com.hpote.backend.common.BaseEntity;
import com.hpote.backend.professional.models.ProfessionalProfile;

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
@Table(name = "service_responses", uniqueConstraints = @UniqueConstraint(
        name = "uk_service_response_request_professional",
        columnNames = {"service_request_id", "professional_profile_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professional_profile_id", nullable = false)
    private ProfessionalProfile professionalProfile;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quoteAmount;

    @Column(nullable = false, length = 3)
    private String currencyCode;

    @Column(nullable = false, length = 1000)
    private String message;

    private Integer estimatedDays;

    @Column(nullable = false, length = 20)
    private String status;
}
