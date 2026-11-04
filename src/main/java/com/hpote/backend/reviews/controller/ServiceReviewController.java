package com.hpote.backend.reviews.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.reviews.dto.CreateServiceReviewRequest;
import com.hpote.backend.reviews.dto.ServiceReviewView;
import com.hpote.backend.reviews.service.ServiceReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class ServiceReviewController {

    private final ServiceReviewService reviewService;

    @PostMapping("/api/service-requests/{requestId}/review")
    public ResponseEntity<ApiResponseDto<ServiceReviewView>> createReview(
            Principal principal,
            @PathVariable String requestId,
            @Valid @RequestBody CreateServiceReviewRequest input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Review submitted",
                reviewService.create(principal.getName(), requestId, input)));
    }

    @GetMapping("/api/professionals/{professionalId}/reviews")
    public ResponseEntity<ApiResponseDto<List<ServiceReviewView>>> professionalReviews(
            @PathVariable String professionalId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professional reviews",
                reviewService.getProfessionalReviews(professionalId)));
    }
}
