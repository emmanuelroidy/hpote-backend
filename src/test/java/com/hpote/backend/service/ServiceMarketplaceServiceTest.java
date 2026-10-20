package com.hpote.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.service.dto.CreateServiceRequest;
import com.hpote.backend.service.dto.CreateServiceResponse;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceResponse;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.service.repository.ServiceStatusRepository;
import com.hpote.backend.service.service.ServiceMarketplaceService;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ServiceMarketplaceServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionRepository professionRepository;
    @Mock
    private ProfessionalRepository professionalRepository;
    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private ServiceResponseRepository responseRepository;
    @Mock
    private ServiceStatusRepository statusRepository;

    private ServiceMarketplaceService marketplaceService;

    @BeforeEach
    void setUp() {
        marketplaceService = new ServiceMarketplaceService(
                userRepository,
                professionRepository,
                professionalRepository,
                requestRepository,
                responseRepository,
                statusRepository);
    }

    @Test
    void createsAnOpenRequestForTheAuthenticatedUser() {
        User customer = user("customer-id", "customer@example.com", "NORMAL");
        Profession profession = profession("profession-id");
        ServiceStatus open = ServiceStatus.builder().name("OPEN").build();
        CreateServiceRequest input = new CreateServiceRequest();
        input.setProfessionId(profession.getId());
        input.setDescription("  Repair my tap  ");
        input.setPictureUrls(List.of("/api/media/images/first", "/api/media/images/second"));
        input.setLatitude(-6.8);
        input.setLongitude(39.2);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(professionRepository.findById(profession.getId())).thenReturn(Optional.of(profession));
        when(statusRepository.findByName("OPEN")).thenReturn(Optional.of(open));
        when(requestRepository.save(any(ServiceRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = marketplaceService.createRequest(customer.getEmail(), input);

        assertEquals("customer-id", created.getRequesterId());
        assertEquals("OPEN", created.getStatus());
        assertEquals("Repair my tap", created.getDescription());
        assertEquals(List.of("/api/media/images/first", "/api/media/images/second"),
                created.getPictureUrls());
        assertEquals("/api/media/images/first", created.getPictureUrl());
    }

    @Test
    void adminCanCancelOpenRequestButCannotChangeAnAssignedJob() {
        ServiceRequest openRequest = request(profession("profession-id"),
                user("customer-id", "customer@example.com", "NORMAL"));
        openRequest.setId("request-id");
        ServiceStatus cancelled = ServiceStatus.builder().name("CANCELLED").build();
        when(requestRepository.findById("request-id")).thenReturn(Optional.of(openRequest));
        when(statusRepository.findByName("CANCELLED")).thenReturn(Optional.of(cancelled));

        var result = marketplaceService.cancelRequestForAdmin("request-id");

        assertEquals("CANCELLED", result.getStatus());

        openRequest.setStatus(ServiceStatus.builder().name("IN_PROGRESS").build());
        assertThrows(ApiExceptionDto.class,
                () -> marketplaceService.cancelRequestForAdmin("request-id"));
    }

    @Test
    void rejectsAProfessionalResponseOutsideTheirServiceRadius() {
        UserRole role = UserRole.builder().name("PROFESSIONAL").build();
        User professional = user("professional-id", "pro@example.com", role);
        Profession profession = profession("profession-id");
        professional.setProfessions(new HashSet<>(Set.of(profession)));

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(professional)
                .profession(profession)
                .latitude(0.0)
                .longitude(0.0)
                .serviceRadiusKm(5.0)
                .isAvailable(true)
                .build();
        ServiceRequest request = ServiceRequest.builder()
                .profession(profession)
                .latitude(1.0)
                .longitude(1.0)
                .status(ServiceStatus.builder().name("OPEN").build())
                .build();

        when(userRepository.findByEmail(professional.getEmail())).thenReturn(Optional.of(professional));
        when(professionalRepository.findByUserId(professional.getId())).thenReturn(Optional.of(profile));
        when(requestRepository.findById("request-id")).thenReturn(Optional.of(request));

        CreateServiceResponse input = new CreateServiceResponse();
        input.setQuoteAmount(new java.math.BigDecimal("100.00"));
        input.setCurrencyCode("TZS");
        input.setMessage("I can help");

        assertThrows(ApiExceptionDto.class,
                () -> marketplaceService.respond(professional.getEmail(), "request-id", input));
    }

    @Test
    void findsRequestsForEveryProfessionAssignedToTheProfessional() {
        User professional = user("professional-id", "pro@example.com", "PROFESSIONAL");
        Profession plumbing = profession("plumbing-id");
        Profession electrical = profession("electrical-id");
        professional.setProfessions(new HashSet<>(Set.of(plumbing, electrical)));

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(professional)
                .profession(plumbing)
                .latitude(-6.8)
                .longitude(39.2)
                .serviceRadiusKm(25.0)
                .isAvailable(true)
                .build();
        ServiceRequest plumbingRequest = request(plumbing, professional);
        ServiceRequest electricalRequest = request(electrical, professional);

        when(userRepository.findByEmail(professional.getEmail())).thenReturn(Optional.of(professional));
        when(professionalRepository.findByUserId(professional.getId())).thenReturn(Optional.of(profile));
        when(requestRepository.findByStatus_NameAndProfession_IdOrderByCreatedAtDesc(
                "OPEN", plumbing.getId())).thenReturn(List.of(plumbingRequest));
        when(requestRepository.findByStatus_NameAndProfession_IdOrderByCreatedAtDesc(
                "OPEN", electrical.getId())).thenReturn(List.of(electricalRequest));

        var available = marketplaceService.getAvailableRequests(professional.getEmail());

        assertEquals(2, available.size());
    }

    @Test
    void acceptingAResponseDeclinesTheOtherQuotesAndAssignsTheProfessional() {
        User customer = user("customer-id", "customer@example.com", "NORMAL");
        User professional = user("professional-id", "pro@example.com", "PROFESSIONAL");
        Profession profession = profession("profession-id");
        ServiceStatus acceptedStatus = ServiceStatus.builder().name("ACCEPTED").build();
        ServiceRequest request = request(profession, customer);
        request.setId("request-id");
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(professional)
                .profession(profession)
                .build();
        profile.setId("profile-id");
        var accepted = response("response-id", request, profile);
        var competing = response("other-response-id", request,
                ProfessionalProfile.builder()
                        .user(user("other-professional-id", "other@example.com", "PROFESSIONAL"))
                        .profession(profession)
                        .build());

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdAndRequesterId("request-id", customer.getId()))
                .thenReturn(Optional.of(request));
        when(responseRepository.findByIdAndServiceRequestId("response-id", "request-id"))
                .thenReturn(Optional.of(accepted));
        when(responseRepository.findByServiceRequestIdOrderByCreatedAtAsc("request-id"))
                .thenReturn(List.of(accepted, competing));
        when(statusRepository.findByName("ACCEPTED")).thenReturn(Optional.of(acceptedStatus));

        var result = marketplaceService.acceptResponse(
                customer.getEmail(), "request-id", "response-id");

        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("professional-id", result.getAssignedProfessionalId());
        assertEquals("ACCEPTED", accepted.getStatus());
        assertEquals("DECLINED", competing.getStatus());
    }

    @Test
    void returnsReciprocalNavigationDestinationsOnlyForAcceptedRequestParties() {
        User customer = user("customer-id", "customer@example.com", "NORMAL");
        User professional = user("professional-id", "pro@example.com", "PROFESSIONAL");
        Profession profession = profession("profession-id");
        ServiceRequest acceptedRequest = request(profession, customer);
        acceptedRequest.setId("request-id");
        acceptedRequest.setStatus(ServiceStatus.builder().name("ACCEPTED").build());
        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(professional)
                .profession(profession)
                .latitude(-6.75)
                .longitude(39.25)
                .build();
        ServiceResponse accepted = response("response-id", acceptedRequest, profile);
        accepted.setStatus("ACCEPTED");

        when(requestRepository.findById("request-id")).thenReturn(Optional.of(acceptedRequest));
        when(responseRepository.findByServiceRequestIdAndStatus("request-id", "ACCEPTED"))
                .thenReturn(Optional.of(accepted));
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(userRepository.findByEmail(professional.getEmail())).thenReturn(Optional.of(professional));

        var customerRoute = marketplaceService.getNavigationDetails(
                customer.getEmail(), "request-id");
        var professionalRoute = marketplaceService.getNavigationDetails(
                professional.getEmail(), "request-id");

        assertEquals("Test user's saved location", customerRoute.getDestinationName());
        assertEquals(-6.75, customerRoute.getDestinationLatitude());
        assertEquals(39.25, customerRoute.getDestinationLongitude());
        assertEquals("Test user's service location", professionalRoute.getDestinationName());
        assertEquals(-6.8, professionalRoute.getDestinationLatitude());
        assertEquals(39.2, professionalRoute.getDestinationLongitude());

        User stranger = user("stranger-id", "stranger@example.com", "NORMAL");
        when(userRepository.findByEmail(stranger.getEmail())).thenReturn(Optional.of(stranger));
        assertThrows(ApiExceptionDto.class,
                () -> marketplaceService.getNavigationDetails(stranger.getEmail(), "request-id"));
    }

    private static ServiceResponse response(
            String id, ServiceRequest request, ProfessionalProfile profile) {
        var response = ServiceResponse.builder()
                .serviceRequest(request)
                .professionalProfile(profile)
                .status("SUBMITTED")
                .build();
        response.setId(id);
        return response;
    }

    private static ServiceRequest request(Profession profession, User requester) {
        ServiceRequest request = ServiceRequest.builder()
                .requester(requester)
                .profession(profession)
                .description("Please help")
                .latitude(-6.8)
                .longitude(39.2)
                .status(ServiceStatus.builder().name("OPEN").build())
                .build();
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    private static User user(String id, String email, String roleName) {
        return user(id, email, UserRole.builder().name(roleName).build());
    }

    private static User user(String id, String email, UserRole role) {
        User user = User.builder()
                .name("Test user")
                .phone("255700000000")
                .email(email)
                .role(role)
                .professions(new HashSet<>())
                .build();
        user.setId(id);
        return user;
    }

    private static Profession profession(String id) {
        Profession profession = Profession.builder()
                .name("PLUMBER")
                .isActive(true)
                .build();
        profession.setId(id);
        return profession;
    }
}
