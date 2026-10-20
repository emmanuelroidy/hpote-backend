package com.hpote.backend.service.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hpote.backend.service.models.ServiceRequest;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceRequestView {
    String id;
    String requesterId;
    String requesterName;
    String professionId;
    String professionName;
    String description;
    String pictureUrl;
    List<String> pictureUrls;
    Double latitude;
    Double longitude;
    String status;
    String assignedProfessionalId;
    LocalDateTime createdAt;

    public static ServiceRequestView from(ServiceRequest request, String assignedProfessionalId) {
        List<String> pictureUrls = new ArrayList<>();
        if (request.getPictureUrl() != null && !request.getPictureUrl().isBlank()) {
            pictureUrls.add(request.getPictureUrl());
        }
        if (request.getAdditionalPictureUrls() != null) {
            request.getAdditionalPictureUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .filter(url -> !pictureUrls.contains(url))
                    .forEach(pictureUrls::add);
        }
        return ServiceRequestView.builder()
                .id(request.getId())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .professionId(request.getProfession().getId())
                .professionName(request.getProfession().getName())
                .description(request.getDescription())
                .pictureUrl(request.getPictureUrl())
                .pictureUrls(pictureUrls)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status(request.getStatus().getName())
                .assignedProfessionalId(assignedProfessionalId)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
