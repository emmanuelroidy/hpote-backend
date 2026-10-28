package com.hpote.backend.service.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.service.dto.CreateServiceRequest;
import com.hpote.backend.service.dto.CreateServiceResponse;
import com.hpote.backend.service.dto.RequestNavigationView;
import com.hpote.backend.service.dto.ServiceRequestView;
import com.hpote.backend.service.dto.ServiceResponseView;
import com.hpote.backend.service.service.ServiceMarketplaceService;
import com.hpote.backend.service.service.RoutePlanningService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@Validated
public class ServiceRequestController {

    private final ServiceMarketplaceService marketplaceService;
    private final RoutePlanningService routingService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> createRequest(
            Principal principal, @Valid @RequestBody CreateServiceRequest input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service request created",
                marketplaceService.createRequest(principal.getName(), input)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponseDto<List<ServiceRequestView>>> getMyRequests(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Your service requests",
                marketplaceService.getMyRequests(principal.getName())));
    }

    @GetMapping("/{requestId}/navigation")
    public ResponseEntity<ApiResponseDto<RequestNavigationView>> getNavigationDetails(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Navigation details retrieved",
                marketplaceService.getNavigationDetails(principal.getName(), requestId)));
    }

    @GetMapping("/{requestId}/route")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getRoute(
            Principal principal,
            @PathVariable String requestId,
            @RequestParam double fromLatitude,
            @RequestParam double fromLongitude,
            @RequestParam String profile,
            @RequestParam(defaultValue = "recommended") String weighting) {
        RequestNavigationView destination =
                marketplaceService.getNavigationDetails(principal.getName(), requestId);
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Route retrieved",
                routingService.route(
                        fromLatitude,
                        fromLongitude,
                        destination.getDestinationLatitude(),
                        destination.getDestinationLongitude(),
                        profile,
                        weighting)));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponseDto<List<ServiceRequestView>>> getAvailableRequests(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Available nearby requests",
                marketplaceService.getAvailableRequests(principal.getName())));
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponseDto<List<ServiceRequestView>>> getAssignedRequests(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Your assigned service requests",
                marketplaceService.getAssignedRequests(principal.getName())));
    }

    @GetMapping("/my-responses")
    public ResponseEntity<ApiResponseDto<List<ServiceResponseView>>> getMyResponses(Principal principal) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Your professional responses",
                marketplaceService.getMyResponses(principal.getName())));
    }

    @GetMapping("/{requestId}/responses")
    public ResponseEntity<ApiResponseDto<List<ServiceResponseView>>> getResponses(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Responses retrieved",
                marketplaceService.getResponses(principal.getName(), requestId)));
    }

    @PostMapping("/{requestId}/responses")
    public ResponseEntity<ApiResponseDto<ServiceResponseView>> respond(
            Principal principal,
            @PathVariable String requestId,
            @Valid @RequestBody CreateServiceResponse input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Response submitted",
                marketplaceService.respond(principal.getName(), requestId, input)));
    }

    @PostMapping("/{requestId}/responses/{responseId}/accept")
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> acceptResponse(
            Principal principal,
            @PathVariable String requestId,
            @PathVariable String responseId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Professional selected",
                marketplaceService.acceptResponse(principal.getName(), requestId, responseId)));
    }

    @PostMapping("/{requestId}/start")
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> startRequest(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service started",
                marketplaceService.startRequest(principal.getName(), requestId)));
    }

    @PostMapping("/{requestId}/complete")
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> completeRequest(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service completed",
                marketplaceService.completeRequest(principal.getName(), requestId)));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponseDto<ServiceRequestView>> cancelRequest(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Service request cancelled",
                marketplaceService.cancelRequest(principal.getName(), requestId)));
    }
}
