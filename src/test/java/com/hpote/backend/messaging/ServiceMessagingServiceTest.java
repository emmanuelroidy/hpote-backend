package com.hpote.backend.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.messaging.dto.ServiceMessageRequest;
import com.hpote.backend.messaging.model.ServiceMessage;
import com.hpote.backend.messaging.repository.ServiceMessageRepository;
import com.hpote.backend.messaging.service.ServiceMessagingService;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceResponse;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ServiceMessagingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private ServiceResponseRepository responseRepository;
    @Mock
    private ServiceMessageRepository messageRepository;
    @Mock
    private NotificationService notificationService;

    private ServiceMessagingService messagingService;
    private User customer;
    private User professional;
    private ServiceRequest request;

    @BeforeEach
    void setUp() {
        messagingService = new ServiceMessagingService(
                userRepository,
                requestRepository,
                responseRepository,
                messageRepository,
                notificationService);

        customer = user("customer-id", "customer@example.com");
        professional = user("professional-id", "professional@example.com");
        Profession profession = Profession.builder().name("PLUMBER").build();
        request = ServiceRequest.builder()
                .requester(customer)
                .profession(profession)
                .status(ServiceStatus.builder().name("ACCEPTED").build())
                .build();
        request.setId("request-id");

        ProfessionalProfile profile = ProfessionalProfile.builder()
                .user(professional)
                .profession(profession)
                .build();
        ServiceResponse accepted = ServiceResponse.builder()
                .serviceRequest(request)
                .professionalProfile(profile)
                .status("ACCEPTED")
                .build();
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(responseRepository.findByServiceRequestIdAndStatus(request.getId(), "ACCEPTED"))
                .thenReturn(Optional.of(accepted));
    }

    @Test
    void sendsAServiceMessageAndNotifiesTheOtherParty() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(messageRepository.save(any(ServiceMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ServiceMessageRequest input = new ServiceMessageRequest();
        input.setContent("  I will arrive at 10.  ");

        var sent = messagingService.sendMessage(customer.getEmail(), request.getId(), input);

        assertEquals("I will arrive at 10.", sent.getContent());
        assertEquals(customer.getId(), sent.getSenderId());
        verify(notificationService).create(
                professional, "NEW_MESSAGE", "New message",
                "Customer sent you a message about PLUMBER", request.getId());
    }

    @Test
    void onlyTheCustomerAndAssignedProfessionalCanReadTheChat() {
        User otherUser = user("other-id", "other@example.com");
        when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));

        assertThrows(ApiExceptionDto.class,
                () -> messagingService.getMessages(otherUser.getEmail(), request.getId()));
    }

    @Test
    void cannotSendMessagesBeforeAProfessionalIsSelected() {
        request.setStatus(ServiceStatus.builder().name("OPEN").build());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        ServiceMessageRequest input = new ServiceMessageRequest();
        input.setContent("Can you help?");

        assertThrows(ApiExceptionDto.class,
                () -> messagingService.sendMessage(customer.getEmail(), request.getId(), input));
    }

    @Test
    void readingTheChatMarksIncomingMessagesRead() {
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        ServiceMessage incoming = ServiceMessage.builder()
                .serviceRequest(request)
                .sender(professional)
                .content("I am on my way.")
                .build();
        incoming.setId("message-id");
        when(messageRepository.findTop100ByServiceRequestIdOrderByCreatedAtDesc(request.getId()))
                .thenReturn(List.of(incoming));

        var messages = messagingService.getMessages(customer.getEmail(), request.getId());

        assertEquals(1, messages.size());
        assertNotNull(incoming.getReadAt());
    }

    private User user(String id, String email) {
        String name = email.substring(0, email.indexOf('@'));
        User user = User.builder()
                .name(Character.toUpperCase(name.charAt(0)) + name.substring(1))
                .email(email)
                .build();
        user.setId(id);
        return user;
    }
}
