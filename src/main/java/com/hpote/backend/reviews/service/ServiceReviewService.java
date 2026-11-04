package com.hpote.backend.reviews.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.reviews.dto.CreateServiceReviewRequest;
import com.hpote.backend.reviews.dto.ServiceReviewView;
import com.hpote.backend.reviews.model.ServiceReview;
import com.hpote.backend.reviews.repository.ServiceReviewRepository;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceResponse;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.service.repository.ServiceResponseRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceReviewService {

    private final UserRepository userRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceResponseRepository responseRepository;
    private final ServiceReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Transactional
    public ServiceReviewView create(
            String username, String requestId, CreateServiceReviewRequest input) {
        User reviewer = findUser(username);
        ServiceRequest request = requestRepository.findByIdAndRequesterId(requestId, reviewer.getId())
                .orElseThrow(() -> new ApiExceptionDto("Service request not found"));
        if (!"COMPLETED".equals(request.getStatus().getName())) {
            throw new ApiExceptionDto("Only completed services can be reviewed");
        }
        if (reviewRepository.findByServiceRequestId(requestId).isPresent()) {
            throw new ApiExceptionDto("This service has already been reviewed");
        }
        ServiceResponse accepted = responseRepository.findByServiceRequestIdAndStatus(requestId, "ACCEPTED")
                .orElseThrow(() -> new ApiExceptionDto("This service has no assigned professional"));

        User professional = accepted.getProfessionalProfile().getUser();
        ServiceReview review = reviewRepository.save(ServiceReview.builder()
                .serviceRequest(request)
                .reviewer(reviewer)
                .professional(professional)
                .rating(input.getRating())
                .comment(input.getComment() == null || input.getComment().isBlank()
                        ? null : input.getComment().trim())
                .build());
        List<Object[]> summary = reviewRepository.summarizeByProfessionalIds(List.of(professional.getId()));
        if (!summary.isEmpty()) {
            professional.setRating(((Number) summary.get(0)[1]).doubleValue());
        }
        notificationService.create(professional, "NEW_REVIEW", "You received a review",
                reviewer.getName() + " rated your " + request.getProfession().getName() + " service",
                request.getId());
        return ServiceReviewView.from(review);
    }

    @Transactional(readOnly = true)
    public List<ServiceReviewView> getProfessionalReviews(String professionalId) {
        User professional = userRepository.findById(professionalId)
                .filter(user -> user.getRole() != null
                        && "PROFESSIONAL".equalsIgnoreCase(user.getRole().getName()))
                .orElseThrow(() -> new ApiExceptionDto("Professional not found"));
        return reviewRepository.findTop20ByProfessionalIdOrderByCreatedAtDesc(professional.getId())
                .stream()
                .map(ServiceReviewView::from)
                .toList();
    }

    private User findUser(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
    }
}
