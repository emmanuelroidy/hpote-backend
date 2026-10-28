package com.hpote.backend.service.service;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.service.dto.CreateServiceRequest;
import com.hpote.backend.service.dto.CreateServiceResponse;
import com.hpote.backend.service.dto.RequestNavigationView;
import com.hpote.backend.service.dto.ServiceRequestView;
import com.hpote.backend.service.dto.ServiceResponseView;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceResponse;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.service.repository.ServiceStatusRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceMarketplaceService {

    private final UserRepository userRepository;
    private final ProfessionRepository professionRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceResponseRepository responseRepository;
    private final ServiceStatusRepository statusRepository;

    @Transactional
    public ServiceRequestView createRequest(String username, CreateServiceRequest input) {
        User requester = findUser(username);
        Profession profession = professionRepository.findById(input.getProfessionId())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ApiExceptionDto("Profession not found or inactive"));

        List<String> suppliedPhotos = new ArrayList<>();
        if (input.getPictureUrls() != null) {
            suppliedPhotos.addAll(input.getPictureUrls());
        }
        if (input.getPictureUrl() != null && !input.getPictureUrl().isBlank()) {
            suppliedPhotos.add(0, input.getPictureUrl());
        }
        List<String> pictureUrls = new ArrayList<>(new LinkedHashSet<>(
                suppliedPhotos.stream()
                        .filter(url -> url != null && !url.isBlank())
                        .map(String::trim)
                        .toList()));
        if (pictureUrls.size() > 5) {
            throw new ApiExceptionDto("A service request can include up to 5 photos");
        }

        ServiceRequest request = ServiceRequest.builder()
                .requester(requester)
                .profession(profession)
                .description(input.getDescription().trim())
                .pictureUrl(pictureUrls.isEmpty() ? null : pictureUrls.get(0))
                .additionalPictureUrls(pictureUrls.size() < 2
                        ? new ArrayList<>()
                        : new ArrayList<>(pictureUrls.subList(1, pictureUrls.size())))
                .latitude(input.getLatitude())
                .longitude(input.getLongitude())
                .status(getStatus("OPEN"))
                .build();

        return ServiceRequestView.from(requestRepository.save(request), null);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> getMyRequests(String username) {
        User requester = findUser(username);
        return requestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId())
                .stream()
                .map(request -> ServiceRequestView.from(
                        request, acceptedProfessionalId(request.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> getAllRequestsForAdmin() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(request -> ServiceRequestView.from(
                        request, acceptedProfessionalId(request.getId())))
                .toList();
    }

    @Transactional
    public ServiceRequestView cancelRequestForAdmin(String requestId) {
        ServiceRequest request = findRequest(requestId);
        if (!"OPEN".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("Only an open request can be cancelled by an administrator");
        }
        request.setStatus(getStatus("CANCELLED"));
        return ServiceRequestView.from(request, null);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> getAvailableRequests(String username) {
        ProfessionalProfile profile = getProfessionalProfile(username);
        if (!Boolean.TRUE.equals(profile.getIsAvailable())) {
            throw new ApiExceptionDto("Set your professional profile to available to see requests");
        }

        Set<String> professionIds = profile.getUser().getProfessions().stream()
                .map(Profession::getId)
                .collect(Collectors.toSet());
        if (professionIds.isEmpty()) {
            throw new ApiExceptionDto("Add a profession to your professional account");
        }

        return professionIds.stream()
                .flatMap(professionId -> requestRepository
                        .findByStatus_NameAndProfession_IdOrderByCreatedAtDesc("OPEN", professionId)
                        .stream())
                .filter(request -> isWithinServiceRadius(request, profile))
                .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                .map(request -> ServiceRequestView.from(request, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> getAssignedRequests(String username) {
        ProfessionalProfile profile = getProfessionalProfile(username);
        return responseRepository.findByProfessionalProfileIdAndStatusOrderByCreatedAtDesc(
                        profile.getId(), "ACCEPTED")
                .stream()
                .map(response -> ServiceRequestView.from(
                        response.getServiceRequest(), profile.getUser().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestNavigationView getNavigationDetails(String username, String requestId) {
        User caller = findUser(username);
        ServiceRequest request = findRequest(requestId);
        String status = request.getStatus().getName();
        if (!"ACCEPTED".equals(status) && !"IN_PROGRESS".equals(status)) {
            throw new ApiExceptionDto("Directions are available after a quote is accepted");
        }

        ServiceResponse accepted = responseRepository
                .findByServiceRequestIdAndStatus(requestId, "ACCEPTED")
                .orElseThrow(() -> new ApiExceptionDto("This request has no assigned professional"));
        ProfessionalProfile profile = accepted.getProfessionalProfile();
        boolean isRequester = request.getRequester().getId().equals(caller.getId());
        boolean isAssignedProfessional = profile.getUser().getId().equals(caller.getId());
        if (!isRequester && !isAssignedProfessional) {
            throw new ApiExceptionDto("Only the customer and assigned professional can view directions");
        }
        if (request.getLatitude() == null || request.getLongitude() == null
                || profile.getLatitude() == null || profile.getLongitude() == null) {
            throw new ApiExceptionDto("Both saved locations are required before directions are available");
        }

        return RequestNavigationView.builder()
                .requestId(request.getId())
                .professionName(request.getProfession().getName())
                .requesterName(request.getRequester().getName())
                .professionalName(profile.getUser().getName())
                .status(status)
                .startLatitude(isAssignedProfessional
                        ? profile.getLatitude()
                        : request.getLatitude())
                .startLongitude(isAssignedProfessional
                        ? profile.getLongitude()
                        : request.getLongitude())
                .endLatitude(isAssignedProfessional
                        ? request.getLatitude()
                        : profile.getLatitude())
                .endLongitude(isAssignedProfessional
                        ? request.getLongitude()
                        : profile.getLongitude())
                .destinationName(isAssignedProfessional
                        ? request.getRequester().getName() + "'s service location"
                        : profile.getUser().getName() + "'s saved location")
                .destinationLatitude(isAssignedProfessional
                        ? request.getLatitude()
                        : profile.getLatitude())
                .destinationLongitude(isAssignedProfessional
                        ? request.getLongitude()
                        : profile.getLongitude())
                .requesterLatitude(request.getLatitude())
                .requesterLongitude(request.getLongitude())
                .professionalLatitude(profile.getLatitude())
                .professionalLongitude(profile.getLongitude())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponseView> getMyResponses(String username) {
        ProfessionalProfile profile = getProfessionalProfile(username);
        return responseRepository.findByProfessionalProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(ServiceResponseView::from)
                .toList();
    }

    @Transactional
    public ServiceResponseView respond(
            String username, String requestId, CreateServiceResponse input) {
        ProfessionalProfile profile = getProfessionalProfile(username);
        if (!Boolean.TRUE.equals(profile.getIsAvailable())) {
            throw new ApiExceptionDto("Your professional profile is not available");
        }

        ServiceRequest request = findRequest(requestId);
        if (!"OPEN".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("This request is no longer accepting responses");
        }
        boolean hasProfession = profile.getUser().getProfessions().stream()
                .anyMatch(profession -> profession.getId().equals(request.getProfession().getId()));
        if (!hasProfession) {
            throw new ApiExceptionDto("Your profession does not match this request");
        }
        if (!isWithinServiceRadius(request, profile)) {
            throw new ApiExceptionDto("This request is outside your service radius");
        }
        if (responseRepository.existsByServiceRequestIdAndProfessionalProfileId(
                requestId, profile.getId())) {
            throw new ApiExceptionDto("You have already responded to this request");
        }

        ServiceResponse response = ServiceResponse.builder()
                .serviceRequest(request)
                .professionalProfile(profile)
                .quoteAmount(input.getQuoteAmount())
                .currencyCode(input.getCurrencyCode())
                .message(input.getMessage().trim())
                .estimatedDays(input.getEstimatedDays())
                .status("SUBMITTED")
                .build();
        return ServiceResponseView.from(responseRepository.save(response));
    }

    @Transactional(readOnly = true)
    public List<ServiceResponseView> getResponses(String username, String requestId) {
        User requester = findUser(username);
        requestRepository.findByIdAndRequesterId(requestId, requester.getId())
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));

        return responseRepository.findByServiceRequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(ServiceResponseView::from)
                .toList();
    }

    @Transactional
    public ServiceRequestView acceptResponse(
            String username, String requestId, String responseId) {
        User requester = findUser(username);
        ServiceRequest request = requestRepository.findByIdAndRequesterId(requestId, requester.getId())
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));
        if (!"OPEN".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("This request cannot accept another response");
        }

        ServiceResponse accepted = responseRepository.findByIdAndServiceRequestId(responseId, requestId)
                .orElseThrow(() -> new ApiExceptionDto("Response not found"));
        if (!"SUBMITTED".equals(accepted.getStatus())) {
            throw new ApiExceptionDto("This response is no longer available");
        }

        responseRepository.findByServiceRequestIdOrderByCreatedAtAsc(requestId)
                .forEach(response -> response.setStatus(
                        response.getId().equals(accepted.getId()) ? "ACCEPTED" : "DECLINED"));
        request.setStatus(getStatus("ACCEPTED"));
        return ServiceRequestView.from(request, accepted.getProfessionalProfile().getUser().getId());
    }

    @Transactional
    public ServiceRequestView startRequest(String username, String requestId) {
        ServiceRequest request = findRequest(requestId);
        ServiceResponse accepted = getAssignedResponse(requestId, username);
        if (!"ACCEPTED".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("Only an accepted request can be started");
        }
        request.setStatus(getStatus("IN_PROGRESS"));
        return ServiceRequestView.from(request, accepted.getProfessionalProfile().getUser().getId());
    }

    @Transactional
    public ServiceRequestView completeRequest(String username, String requestId) {
        ServiceRequest request = findRequest(requestId);
        ServiceResponse accepted = getAssignedResponse(requestId, username);
        if (!"IN_PROGRESS".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("Only an in-progress request can be completed");
        }
        request.setStatus(getStatus("COMPLETED"));
        return ServiceRequestView.from(request, accepted.getProfessionalProfile().getUser().getId());
    }

    @Transactional
    public ServiceRequestView cancelRequest(String username, String requestId) {
        User requester = findUser(username);
        ServiceRequest request = requestRepository.findByIdAndRequesterId(requestId, requester.getId())
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));
        if (!"OPEN".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("Only an open request can be cancelled");
        }
        request.setStatus(getStatus("CANCELLED"));
        return ServiceRequestView.from(request, null);
    }

    private User findUser(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
    }

    private ProfessionalProfile getProfessionalProfile(String username) {
        User user = findUser(username);
        if (!"PROFESSIONAL".equalsIgnoreCase(user.getRole().getName())) {
            throw new ApiExceptionDto("A professional account is required");
        }
        return professionalRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiExceptionDto("Set up your professional profile first"));
    }

    private ServiceRequest findRequest(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));
    }

    private ServiceStatus getStatus(String name) {
        return statusRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("Service status is not configured: " + name));
    }

    private ServiceResponse getAssignedResponse(String requestId, String username) {
        User professional = findUser(username);
        ServiceResponse accepted = responseRepository.findByServiceRequestIdAndStatus(requestId, "ACCEPTED")
                .orElseThrow(() -> new ApiExceptionDto("This request has no assigned professional"));
        if (!accepted.getProfessionalProfile().getUser().getId().equals(professional.getId())) {
            throw new ApiExceptionDto("Only the assigned professional can update this request");
        }
        return accepted;
    }

    private String acceptedProfessionalId(String requestId) {
        return responseRepository.findByServiceRequestIdAndStatus(requestId, "ACCEPTED")
                .map(response -> response.getProfessionalProfile().getUser().getId())
                .orElse(null);
    }

    private boolean isWithinServiceRadius(ServiceRequest request, ProfessionalProfile profile) {
        if (request.getLatitude() == null || request.getLongitude() == null) {
            return true;
        }
        if (profile.getLatitude() == null || profile.getLongitude() == null
                || profile.getServiceRadiusKm() == null) {
            return false;
        }
        return distanceKm(
                request.getLatitude(), request.getLongitude(),
                profile.getLatitude(), profile.getLongitude()) <= profile.getServiceRadiusKm();
    }

    private double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double deltaLatitude = Math.toRadians(latitude2 - latitude1);
        double deltaLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        double boundedA = Math.min(1.0, Math.max(0.0, a));
        return 6371.0 * 2 * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA));
    }
}
