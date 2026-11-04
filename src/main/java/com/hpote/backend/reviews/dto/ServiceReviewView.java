package com.hpote.backend.reviews.dto;

import java.time.LocalDateTime;
import com.hpote.backend.reviews.model.ServiceReview;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceReviewView {
    String id;
    String serviceRequestId;
    String reviewerName;
    String professionalId;
    String professionalName;
    String professionName;
    Integer rating;
    String comment;
    LocalDateTime createdAt;

    public static ServiceReviewView from(ServiceReview review) {
        return ServiceReviewView.builder()
                .id(review.getId())
                .serviceRequestId(review.getServiceRequest().getId())
                .reviewerName(review.getReviewer().getName())
                .professionalId(review.getProfessional().getId())
                .professionalName(review.getProfessional().getName())
                .professionName(review.getServiceRequest().getProfession().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
