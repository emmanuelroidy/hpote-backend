package com.hpote.backend.community.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.hpote.backend.service.models.ServiceRequest;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityPostView {
    String id;
    String requesterName;
    String professionId;
    String professionName;
    String description;
    String pictureUrl;
    List<String> pictureUrls;
    List<String> tags;
    String status;
    String distanceLabel;
    LocalDateTime createdAt;
    long likeCount;
    boolean likedByMe;
    List<RecommendationView> recommendations;

    @Value
    @Builder
    public static class RecommendationView {
        String professionalId;
        String professionalName;
        String professionName;
        String profilePhoto;
        String note;
        LocalDateTime createdAt;
    }

    public static CommunityPostView from(
            ServiceRequest request,
            String distanceLabel,
            long likeCount,
            boolean likedByMe,
            List<RecommendationView> recommendations) {
        List<String> pictureUrls = new java.util.ArrayList<>();
        if (request.getPictureUrl() != null && !request.getPictureUrl().isBlank()) {
            pictureUrls.add(request.getPictureUrl());
        }
        if (request.getAdditionalPictureUrls() != null) {
            request.getAdditionalPictureUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .filter(url -> !pictureUrls.contains(url))
                    .forEach(pictureUrls::add);
        }
        return CommunityPostView.builder()
                .id(request.getId())
                .requesterName("HpOtE neighbor")
                .professionId(request.getProfession().getId())
                .professionName(request.getProfession().getName())
                .description(request.getDescription())
                .pictureUrl(request.getPictureUrl())
                .pictureUrls(pictureUrls)
                .tags(request.getTags() == null ? List.of() : request.getTags())
                .status(request.getStatus().getName())
                .distanceLabel(distanceLabel)
                .createdAt(request.getCreatedAt())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .recommendations(recommendations)
                .build();
    }
}
