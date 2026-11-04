package com.hpote.backend.reviews;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.reviews.dto.CreateServiceReviewRequest;
import com.hpote.backend.reviews.repository.ServiceReviewRepository;
import com.hpote.backend.reviews.service.ServiceReviewService;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ServiceReviewServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private ServiceResponseRepository responseRepository;
    @Mock
    private ServiceReviewRepository reviewRepository;
    @Mock
    private NotificationService notificationService;

    private ServiceReviewService reviewService;
    private User customer;

    @BeforeEach
    void setUp() {
        reviewService = new ServiceReviewService(
                userRepository,
                requestRepository,
                responseRepository,
                reviewRepository,
                notificationService);
        customer = User.builder().name("Customer").email("customer@example.com").build();
        customer.setId("customer-id");
    }

    @Test
    void aCustomerCannotReviewAnIncompleteService() {
        ServiceRequest request = ServiceRequest.builder()
                .requester(customer)
                .status(ServiceStatus.builder().name("IN_PROGRESS").build())
                .build();
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdAndRequesterId("request-id", customer.getId()))
                .thenReturn(Optional.of(request));
        CreateServiceReviewRequest input = new CreateServiceReviewRequest();
        input.setRating(5);

        assertThrows(ApiExceptionDto.class,
                () -> reviewService.create(customer.getEmail(), "request-id", input));
    }
}
