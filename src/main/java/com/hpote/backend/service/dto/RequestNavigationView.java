package com.hpote.backend.service.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequestNavigationView {
    String requestId;
    String professionName;
    String requesterName;
    String professionalName;
    String status;
    Double startLatitude;
    Double startLongitude;
    Double endLatitude;
    Double endLongitude;
    String destinationName;
    Double destinationLatitude;
    Double destinationLongitude;
    Double requesterLatitude;
    Double requesterLongitude;
    Double professionalLatitude;
    Double professionalLongitude;
}
