package com.hpote.backend.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hpote.backend.service.models.ServiceResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceResponseView {
    String id;
    String serviceRequestId;
    String requestDescription;
    String professionalId;
    String professionalName;
    BigDecimal quoteAmount;
    String currencyCode;
    String message;
    Integer estimatedDays;
    String status;
    LocalDateTime createdAt;

    public static ServiceResponseView from(ServiceResponse response) {
        var profile = response.getProfessionalProfile();
        return ServiceResponseView.builder()
                .id(response.getId())
                .serviceRequestId(response.getServiceRequest().getId())
                .requestDescription(response.getServiceRequest().getDescription())
                .professionalId(profile.getUser().getId())
                .professionalName(profile.getUser().getName())
                .quoteAmount(response.getQuoteAmount())
                .currencyCode(response.getCurrencyCode())
                .message(response.getMessage())
                .estimatedDays(response.getEstimatedDays())
                .status(response.getStatus())
                .createdAt(response.getCreatedAt())
                .build();
    }
}
