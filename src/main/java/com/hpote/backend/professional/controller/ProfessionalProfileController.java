package com.hpote.backend.professional.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.professional.dto.ProfessionalProfileRequest;
import com.hpote.backend.professional.dto.ProfessionalProfileView;
import com.hpote.backend.professional.service.ProfessionalProfileService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/professionals/profile")
@RequiredArgsConstructor
@Validated
public class ProfessionalProfileController {

    private final ProfessionalProfileService profileService;

    @PutMapping
    public ResponseEntity<ApiResponseDto<ProfessionalProfileView>> update(
            Principal principal, @Valid @RequestBody ProfessionalProfileRequest input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professional profile saved",
                profileService.updateProfile(principal.getName(), input)));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<ProfessionalProfileView>> getMine(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professional profile retrieved",
                profileService.getMyProfile(principal.getName())));
    }
}
