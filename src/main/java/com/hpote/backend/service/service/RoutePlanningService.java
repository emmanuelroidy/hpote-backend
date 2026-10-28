package com.hpote.backend.service.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hpote.backend.common.ApiExceptionDto;

@Service
public class RoutePlanningService {

    private static final Set<String> PROFILES = Set.of("car", "bike", "foot");
    private static final Set<String> WEIGHTINGS = Set.of("recommended", "fastest", "shortest");

    private final RestClient restClient;

    @Autowired
    public RoutePlanningService(
            RestClient.Builder restClientBuilder,
            @Value("${app.routing.base-url:https://valhalla1.openstreetmap.de}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this(restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build());
    }

    public RoutePlanningService(RestClient restClient) {
        this.restClient = restClient;
    }

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

        String costing = switch (profile) {
            case "bike" -> "bicycle";
            case "foot" -> "pedestrian";
            default -> "auto";
        };
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("locations", List.of(
                Map.of("lat", fromLatitude, "lon", fromLongitude),
                Map.of("lat", toLatitude, "lon", toLongitude)));
        request.put("costing", costing);
        request.put("units", "kilometers");
        request.put("directions_options", Map.of("units", "kilometers", "language", "en-US"));
        if (!"recommended".equals(weighting)) {
            request.put("alternates", 1);
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return normalizeResponse(response, weighting);
        } catch (RestClientException exception) {
            throw new ApiExceptionDto("Could not get directions right now. Please try again.");
        }
    }

    private Map<String, Object> normalizeResponse(Map<String, Object> response, String weighting) {
        Map<String, Object> trip = map(response == null ? null : response.get("trip"));
        if (trip.isEmpty()) {
            throw new ApiExceptionDto("The routing service returned no route");
        }
        if (trip.get("status") instanceof Number status && status.intValue() != 0) {
            throw new ApiExceptionDto("The routing service could not find a route");
        }

        List<Map<String, Object>> candidates = new ArrayList<>();
        candidates.add(trip);
        Object alternatesValue = response.get("alternates");
        if (alternatesValue instanceof List<?> alternates) {
            for (Object alternate : alternates) {
                Map<String, Object> alternativeTrip = map(map(alternate).get("trip"));
                if (!alternativeTrip.isEmpty()) {
                    candidates.add(alternativeTrip);
                }
            }
        }

        Map<String, Object> selected = candidates.get(0);
        if ("fastest".equals(weighting) || "shortest".equals(weighting)) {
            String metric = "fastest".equals(weighting) ? "time" : "length";
            selected = candidates.stream()
                    .min((first, second) -> Double.compare(
                            requiredNumber(map(first.get("summary")).get(metric)),
                            requiredNumber(map(second.get("summary")).get(metric))))
                    .orElse(selected);
        }

        Map<String, Object> summary = map(selected.get("summary"));
        List<?> legs = list(selected.get("legs"));
        if (legs.isEmpty()) {
            throw new ApiExceptionDto("The routing service returned no route");
        }
        Map<String, Object> leg = map(legs.get(0));
        List<List<Double>> coordinates = decodePolyline6(leg.get("shape"));
        if (coordinates.isEmpty()) {
            throw new ApiExceptionDto("The routing service returned no route geometry");
        }

        List<Map<String, Object>> instructions = new ArrayList<>();
        for (Object maneuverValue : list(leg.get("maneuvers"))) {
            Map<String, Object> maneuver = map(maneuverValue);
            Map<String, Object> instruction = new LinkedHashMap<>();
            instruction.put("text", maneuver.getOrDefault("instruction", "Continue"));
            instruction.put("distance", requiredNumber(maneuver.get("length")) * 1000);
            instruction.put("time", requiredNumber(maneuver.get("time")) * 1000);
            instructions.add(instruction);
        }

        Map<String, Object> points = Map.of(
                "type", "LineString",
                "coordinates", coordinates);
        Map<String, Object> path = Map.of(
                "distance", requiredNumber(summary.get("length")) * 1000,
                "time", requiredNumber(summary.get("time")) * 1000,
                "points", points,
                "instructions", instructions);
        return Map.of("route", Map.of("paths", List.of(path)));
    }

    private List<List<Double>> decodePolyline6(Object encodedValue) {
        if (!(encodedValue instanceof String encoded) || encoded.isEmpty()) {
            return List.of();
        }
        List<List<Double>> coordinates = new ArrayList<>();
        int[] index = {0};
        long latitude = 0;
        long longitude = 0;
        while (index[0] < encoded.length()) {
            latitude += decodeValue(encoded, index);
            longitude += decodeValue(encoded, index);
            coordinates.add(List.of(longitude / 1_000_000.0, latitude / 1_000_000.0));
        }
        return coordinates;
    }

    private long decodeValue(String encoded, int[] index) {
        long result = 0;
        int shift = 0;
        int chunk;
        do {
            if (index[0] >= encoded.length() || shift >= 65) {
                throw new ApiExceptionDto("The routing service returned invalid route geometry");
            }
            chunk = encoded.charAt(index[0]++) - 63;
            if (chunk < 0 || chunk > 63) {
                throw new ApiExceptionDto("The routing service returned invalid route geometry");
            }
            result |= (long) (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20);
        return (result & 1) != 0 ? ~(result >> 1) : result >> 1;
    }

    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, item);
            }
        });
        return result;
    }

    private List<?> list(Object value) {
        return value instanceof List<?> result ? result : List.of();
    }

    private double requiredNumber(Object value) {
        if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
            return number.doubleValue();
        }
        throw new ApiExceptionDto("The routing service returned incomplete route data");
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ApiExceptionDto("The route contains invalid coordinates");
        }
    }
}
