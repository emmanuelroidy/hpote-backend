package com.hpote.backend.messaging.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.messaging.dto.ServiceMessageRequest;
import com.hpote.backend.messaging.dto.ServiceMessageView;
import com.hpote.backend.messaging.model.ServiceMessage;
import com.hpote.backend.messaging.repository.ServiceMessageRepository;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceResponse;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceMessagingService {

    private final UserRepository userRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceResponseRepository responseRepository;
    private final ServiceMessageRepository messageRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<ServiceMessageView> getMessages(String username, String requestId) {
        User caller = findUser(username);
        ServiceRequest request = findRequest(requestId);
        getChatPartner(request, caller);
        ensureChatAvailable(request);
        List<ServiceMessage> messages = new ArrayList<>(messageRepository
                .findTop100ByServiceRequestIdOrderByCreatedAtDesc(requestId));
        java.util.Collections.reverse(messages);
        return messages.stream()
                .peek(message -> {
                    if (message.getReadAt() == null && !message.getSender().getId().equals(caller.getId())) {
                        message.setReadAt(LocalDateTime.now());
                    }
                })
                .map(ServiceMessageView::from)
                .toList();
    }

    @Transactional
    public ServiceMessageView sendMessage(
            String username, String requestId, ServiceMessageRequest input) {
        User sender = findUser(username);
        ServiceRequest request = findRequest(requestId);
        User recipient = getChatPartner(request, sender);
        ensureChatAvailable(request);

        ServiceMessage message = messageRepository.save(ServiceMessage.builder()
                .serviceRequest(request)
                .sender(sender)
                .content(input.getContent().trim())
                .build());
        notificationService.create(recipient, "NEW_MESSAGE", "New message",
                sender.getName() + " sent you a message about " + request.getProfession().getName(),
                request.getId());
        return ServiceMessageView.from(message);
    }

    private void ensureChatAvailable(ServiceRequest request) {
        String status = request.getStatus().getName();
        if (!"ACCEPTED".equals(status) && !"IN_PROGRESS".equals(status) && !"COMPLETED".equals(status)) {
            throw new ApiExceptionDto("Messaging is available after a professional is selected");
        }
    }

    private User getChatPartner(ServiceRequest request, User caller) {
        ServiceResponse accepted = responseRepository.findByServiceRequestIdAndStatus(
                        request.getId(), "ACCEPTED")
                .orElseThrow(() -> new ApiExceptionDto("This request has no assigned professional"));
        User requester = request.getRequester();
        User professional = accepted.getProfessionalProfile().getUser();
        if (requester.getId().equals(caller.getId())) {
            return professional;
        }
        if (professional.getId().equals(caller.getId())) {
            return requester;
        }
        throw new ApiExceptionDto("Only the customer and assigned professional can access this chat");
    }

    private ServiceRequest findRequest(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));
    }

    private User findUser(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
    }
}
