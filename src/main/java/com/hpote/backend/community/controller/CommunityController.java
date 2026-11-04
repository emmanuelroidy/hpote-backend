package com.hpote.backend.community.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.community.dto.CommunityLikeView;
import com.hpote.backend.community.dto.CommunityPostView;
import com.hpote.backend.community.dto.ProfessionalDirectoryView;
import com.hpote.backend.community.dto.RecommendProfessionalRequest;
import com.hpote.backend.community.service.CommunityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Validated
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/feed")
    public ResponseEntity<ApiResponseDto<List<CommunityPostView>>> feed(
            Principal principal,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "25") double radiusKm) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Nearby community posts",
                communityService.feed(principal.getName(), latitude, longitude, radiusKm)));
    }

    @PutMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponseDto<CommunityLikeView>> like(
            Principal principal, @PathVariable String postId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Post liked",
                communityService.like(principal.getName(), postId)));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponseDto<CommunityLikeView>> unlike(
            Principal principal, @PathVariable String postId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Post unliked",
                communityService.unlike(principal.getName(), postId)));
    }

    @PostMapping("/posts/{postId}/recommendations")
    public ResponseEntity<ApiResponseDto<CommunityPostView.RecommendationView>> recommend(
            Principal principal,
            @PathVariable String postId,
            @Valid @RequestBody RecommendProfessionalRequest input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professional recommended",
                communityService.recommend(principal.getName(), postId, input)));
    }

    @GetMapping("/professionals/nearby")
    public ResponseEntity<ApiResponseDto<List<ProfessionalDirectoryView>>> nearbyProfessionals(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "25") double radiusKm,
            @RequestParam(required = false) String professionId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Nearby professionals",
                communityService.nearbyProfessionals(latitude, longitude, radiusKm, professionId)));
    }
}
