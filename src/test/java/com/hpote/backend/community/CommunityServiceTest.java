package com.hpote.backend.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.hpote.backend.community.model.ServiceRequestLike;
import com.hpote.backend.community.repository.ProfessionalRecommendationRepository;
import com.hpote.backend.community.repository.ServiceRequestLikeRepository;
import com.hpote.backend.community.service.CommunityService;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import com.hpote.backend.reviews.repository.ServiceReviewRepository;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.models.ServiceStatus;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfessionalRepository professionalRepository;
    @Mock
    private ServiceRequestRepository requestRepository;
    @Mock
    private ServiceRequestLikeRepository likeRepository;
    @Mock
    private ProfessionalRecommendationRepository recommendationRepository;
    @Mock
    private ServiceReviewRepository reviewRepository;
    @Mock
    private NotificationService notificationService;

    private CommunityService communityService;
    private User caller;
    private ServiceRequest post;

    @BeforeEach
    void setUp() {
        communityService = new CommunityService(
                userRepository,
                professionalRepository,
                requestRepository,
                likeRepository,
                recommendationRepository,
                reviewRepository,
                notificationService);

        caller = user("caller-id", "caller@example.com");
        Profession profession = Profession.builder().name("PLUMBER").isActive(true).build();
        profession.setId("plumber-id");
        post = ServiceRequest.builder()
                .requester(user("author-id", "author@example.com"))
                .profession(profession)
                .description("A leaking tap needs repair")
                .latitude(-6.8)
                .longitude(39.2)
                .sharedToCommunity(true)
                .tags(List.of("home repair"))
                .status(ServiceStatus.builder().name("OPEN").build())
                .build();
        post.setId("request-id");
        post.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void feedOnlyReturnsNearbyOptedInOpenPostsWithoutPublicLocationCoordinates() {
        ServiceRequest privatePost = ServiceRequest.builder()
                .requester(post.getRequester())
                .profession(post.getProfession())
                .latitude(-6.8)
                .longitude(39.2)
                .sharedToCommunity(false)
                .status(ServiceStatus.builder().name("OPEN").build())
                .build();
        ServiceRequest distantPost = ServiceRequest.builder()
                .requester(post.getRequester())
                .profession(post.getProfession())
                .latitude(0.0)
                .longitude(0.0)
                .sharedToCommunity(true)
                .status(ServiceStatus.builder().name("OPEN").build())
                .build();
        when(userRepository.findByEmail(caller.getEmail())).thenReturn(Optional.of(caller));
        when(requestRepository.findBySharedToCommunityTrueOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(post, privatePost, distantPost));
        when(likeRepository.countLikesForRequests(List.of(post.getId())))
                .thenReturn(java.util.Collections.singletonList(new Object[] { post.getId(), 2L }));
        when(likeRepository.findByServiceRequestIdInAndUserId(List.of(post.getId()), caller.getId()))
                .thenReturn(List.of());
        when(recommendationRepository.findByServiceRequestIdInOrderByCreatedAtDesc(List.of(post.getId())))
                .thenReturn(List.of());

        var feed = communityService.feed(caller.getEmail(), -6.8, 39.2, 25);

        assertEquals(1, feed.size());
        assertEquals(2, feed.get(0).getLikeCount());
        assertEquals("HpOtE neighbor", feed.get(0).getRequesterName());
        assertEquals("about 1 km away", feed.get(0).getDistanceLabel());
        assertEquals(List.of("home repair"), feed.get(0).getTags());
    }

    @Test
    void likingAnAlreadyLikedPostIsIdempotent() {
        when(userRepository.findByEmail(caller.getEmail())).thenReturn(Optional.of(caller));
        when(requestRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(likeRepository.existsByServiceRequestIdAndUserId(post.getId(), caller.getId()))
                .thenReturn(true);
        when(likeRepository.countLikesForRequests(List.of(post.getId())))
                .thenReturn(java.util.Collections.singletonList(new Object[] { post.getId(), 1L }));

        var result = communityService.like(caller.getEmail(), post.getId());

        assertTrue(result.isLiked());
        assertEquals(1, result.getLikeCount());
        verify(likeRepository, never()).save(any(ServiceRequestLike.class));
    }

    @Test
    void rejectsSearchRadiiAboveThePublicLimit() {
        communityService = new CommunityService(
                userRepository,
                professionalRepository,
                requestRepository,
                likeRepository,
                recommendationRepository,
                reviewRepository,
                notificationService);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.hpote.backend.common.ApiExceptionDto.class,
                () -> communityService.nearbyProfessionals(0, 0, 51, null));
        verify(professionalRepository, never()).findAll();
    }

    private User user(String id, String email) {
        User user = User.builder().name("HpOtE member").email(email).enabled(true).build();
        user.setId(id);
        return user;
    }
}
