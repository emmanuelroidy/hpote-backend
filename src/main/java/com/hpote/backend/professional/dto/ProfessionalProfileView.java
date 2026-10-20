package com.hpote.backend.professional.dto;

import com.hpote.backend.professional.models.ProfessionalProfile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfessionalProfileView {
    String userId;
    String professionId;
    String professionName;
    Double latitude;
    Double longitude;
    Double serviceRadiusKm;
    Boolean isAvailable;
    Boolean isVerified;

    public static ProfessionalProfileView from(ProfessionalProfile profile) {
        return ProfessionalProfileView.builder()
                .userId(profile.getUser().getId())
                .professionId(profile.getProfession().getId())
                .professionName(profile.getProfession().getName())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .serviceRadiusKm(profile.getServiceRadiusKm())
                .isAvailable(profile.getIsAvailable())
                .isVerified(profile.getIsVerified())
                .build();
    }
}
