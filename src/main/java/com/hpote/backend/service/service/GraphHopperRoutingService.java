package com.hpote.backend.service.service;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.hpote.backend.common.ApiExceptionDto;

@Service
public class GraphHopperRoutingService {

    private static final Set<String> PROFILES = Set.of("car", "bike", "foot");
    private static final Set<String> WEIGHTINGS = Set.of("recommended", "fastest", "shortest");

    private final RestClient restClient = RestClient.create();

    @Value("${app.graphhopper.api-key:}")
    private String apiKey;

    public Map<String, Object> route(
            double fromLatitude,
            double fromLongitude,
            double toLatitude,
            double toLongitude,
            String profile,
            String weighting) {
        validateCoordinates(fromLatitude, fromLongitude);
        validateCoordinates(toLatitude, toLongitude);
        if (!PROFILES.contains(profile) || !WEIGHTINGS.contains(weighting)) {
            throw new ApiExceptionDto("Choose a supported travel mode and route preference");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiExceptionDto(
                    "In-app routing is not configured. Set GRAPHHOPPER_API_KEY on the backend.");
        }

        var uri = UriComponentsBuilder
                .fromUriString("https://graphhopper.com/api/1/route")
                .queryParam("point", fromLatitude + "," + fromLongitude)
                .queryParam("point", toLatitude + "," + toLongitude)
                .queryParam("profile", profile)
                .queryParam("weighting", weighting)
                .queryParam("locale", "en")
                .queryParam("instructions", true)
                .queryParam("points_encoded", false)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();
        try {
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !(response.get("paths") instanceof java.util.List<?> paths)
                    || paths.isEmpty()) {
                throw new ApiExceptionDto("GraphHopper returned no route for these locations");
            }
            return Map.of("route", response);
        } catch (RestClientException exception) {
            throw new ApiExceptionDto("Could not get directions right now. Please try again.");
        }
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ApiExceptionDto("The route contains invalid coordinates");
        }
    }
}
