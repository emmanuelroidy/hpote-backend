package com.hpote.backend.community.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfessionalDirectoryView {
    String id;
    String name;
    String profilePhoto;
    String professionName;
    boolean verified;
    double rating;
    long reviewCount;
    long recommendationCount;
    String distanceLabel;
}
