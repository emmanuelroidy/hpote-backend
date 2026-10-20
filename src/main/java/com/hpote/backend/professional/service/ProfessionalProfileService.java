package com.hpote.backend.professional.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.professional.dto.ProfessionalProfileRequest;
import com.hpote.backend.professional.dto.ProfessionalProfileView;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfessionalProfileService {

    private final UserRepository userRepository;
    private final ProfessionRepository professionRepository;
    private final ProfessionalRepository professionalRepository;

    @Transactional
    public ProfessionalProfileView updateProfile(String username, ProfessionalProfileRequest input) {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
        if (!"PROFESSIONAL".equalsIgnoreCase(user.getRole().getName())) {
            throw new ApiExceptionDto("A professional account is required");
        }

        Profession profession = professionRepository.findById(input.getProfessionId())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ApiExceptionDto("Profession not found or inactive"));
        boolean ownsProfession = user.getProfessions().stream()
                .anyMatch(owned -> owned.getId().equals(profession.getId()));
        if (!ownsProfession) {
            throw new ApiExceptionDto("You can only select a profession assigned to your account");
        }

        ProfessionalProfile profile = professionalRepository.findByUserId(user.getId())
                .orElseGet(() -> ProfessionalProfile.builder()
                        .user(user)
                        .isVerified(false)
                        .build());
        profile.setProfession(profession);
        profile.setLatitude(input.getLatitude());
        profile.setLongitude(input.getLongitude());
        profile.setServiceRadiusKm(input.getServiceRadiusKm());
        profile.setIsAvailable(input.getIsAvailable() == null || input.getIsAvailable());

        return ProfessionalProfileView.from(professionalRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public ProfessionalProfileView getMyProfile(String username) {
        User user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
        return professionalRepository.findByUserId(user.getId())
                .map(ProfessionalProfileView::from)
                .orElseThrow(() -> new ApiExceptionDto("Professional profile not found"));
    }
}
