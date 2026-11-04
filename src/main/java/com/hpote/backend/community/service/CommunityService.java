package com.hpote.backend.community.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.common.ApiExceptionDto;
import com.hpote.backend.community.dto.CommunityLikeView;
import com.hpote.backend.community.dto.CommunityPostView;
import com.hpote.backend.community.dto.CommunityPostView.RecommendationView;
import com.hpote.backend.community.dto.ProfessionalDirectoryView;
import com.hpote.backend.community.dto.RecommendProfessionalRequest;
import com.hpote.backend.community.model.ProfessionalRecommendation;
import com.hpote.backend.community.model.ServiceRequestLike;
import com.hpote.backend.community.repository.ProfessionalRecommendationRepository;
import com.hpote.backend.community.repository.ServiceRequestLikeRepository;
import com.hpote.backend.notification.service.NotificationService;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.professional.repository.ProfessionalRepository;
import com.hpote.backend.reviews.repository.ServiceReviewRepository;
import com.hpote.backend.service.models.ServiceRequest;
import com.hpote.backend.service.repository.ServiceRequestRepository;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final int FEED_LIMIT = 100;
    private static final int RESULTS_LIMIT = 40;
    private static final double MAX_RADIUS_KM = 50;

    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRequestRepository requestRepository;
    private final ServiceRequestLikeRepository likeRepository;
    private final ProfessionalRecommendationRepository recommendationRepository;
    private final ServiceReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CommunityPostView> feed(
            String username, double latitude, double longitude, double radiusKm) {
        validateSearch(latitude, longitude, radiusKm);
        User caller = findUser(username);
        List<ServiceRequest> posts = requestRepository
                .findBySharedToCommunityTrueOrderByCreatedAtDesc(PageRequest.of(0, FEED_LIMIT))
                .stream()
                .filter(post -> "OPEN".equals(post.getStatus().getName()))
                .filter(ServiceRequest::isSharedToCommunity)
                .filter(post -> distanceKm(latitude, longitude, post.getLatitude(), post.getLongitude())
                        <= radiusKm)
                .sorted(Comparator.comparing(ServiceRequest::getCreatedAt).reversed())
                .limit(RESULTS_LIMIT)
                .toList();
        if (posts.isEmpty()) return List.of();

        List<String> postIds = posts.stream().map(ServiceRequest::getId).toList();
        Map<String, Long> likeCounts = likeRepository.countLikesForRequests(postIds).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
        Set<String> likedPostIds = likeRepository.findByServiceRequestIdInAndUserId(postIds, caller.getId())
                .stream()
                .map(like -> like.getServiceRequest().getId())
                .collect(Collectors.toSet());
        Map<String, List<RecommendationView>> recommendations = recommendationRepository
                .findByServiceRequestIdInOrderByCreatedAtDesc(postIds).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getServiceRequest().getId(),
                        HashMap::new,
                        Collectors.mapping(this::toRecommendationView, Collectors.toList())));

        return posts.stream()
                .map(post -> CommunityPostView.from(
                        post,
                        distanceLabel(latitude, longitude, post.getLatitude(), post.getLongitude()),
                        likeCounts.getOrDefault(post.getId(), 0L),
                        likedPostIds.contains(post.getId()),
                        recommendations.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CommunityLikeView like(String username, String postId) {
        User caller = findUser(username);
        ServiceRequest post = findCommunityPost(postId);
        if (!likeRepository.existsByServiceRequestIdAndUserId(postId, caller.getId())) {
            likeRepository.save(ServiceRequestLike.builder()
                    .serviceRequest(post)
                    .user(caller)
                    .build());
        }
        return CommunityLikeView.builder()
                .postId(postId)
                .liked(true)
                .likeCount(countLikes(postId))
                .build();
    }

    @Transactional
    public CommunityLikeView unlike(String username, String postId) {
        User caller = findUser(username);
        findCommunityPost(postId);
        likeRepository.findByServiceRequestIdAndUserId(postId, caller.getId())
                .ifPresent(likeRepository::delete);
        return CommunityLikeView.builder()
                .postId(postId)
                .liked(false)
                .likeCount(countLikes(postId))
                .build();
    }

    @Transactional
    public CommunityPostView.RecommendationView recommend(
            String username, String postId, RecommendProfessionalRequest input) {
        User recommender = findUser(username);
        ServiceRequest post = findCommunityPost(postId);
        User professional = userRepository.findById(input.getProfessionalId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new ApiExceptionDto("Professional not found"));
        if (!"PROFESSIONAL".equalsIgnoreCase(professional.getRole().getName())) {
            throw new ApiExceptionDto("You can only recommend a registered professional");
        }
        if (professional.getId().equals(post.getRequester().getId())) {
            throw new ApiExceptionDto("A customer cannot recommend themselves for their own request");
        }
        boolean servesProfession = professional.getProfessions().stream()
                .anyMatch(item -> item.getId().equals(post.getProfession().getId()));
        if (!servesProfession) {
            throw new ApiExceptionDto("The professional does not offer this service");
        }
        if (recommendationRepository.existsByServiceRequestIdAndRecommenderIdAndProfessionalId(
                postId, recommender.getId(), professional.getId())) {
            throw new ApiExceptionDto("You have already recommended this professional for this post");
        }

        ProfessionalRecommendation recommendation = recommendationRepository.save(
                ProfessionalRecommendation.builder()
                        .serviceRequest(post)
                        .recommender(recommender)
                        .professional(professional)
                        .note(input.getNote() == null || input.getNote().isBlank()
                                ? null : input.getNote().trim())
                        .build());
        notificationService.create(post.getRequester(), "PROFESSIONAL_RECOMMENDED",
                "A professional was recommended",
                recommender.getName() + " recommended " + professional.getName()
                        + " for your " + post.getProfession().getName() + " request",
                post.getId());
        return toRecommendationView(recommendation);
    }

    @Transactional(readOnly = true)
    public List<ProfessionalDirectoryView> nearbyProfessionals(
            double latitude, double longitude, double radiusKm, String professionId) {
        validateSearch(latitude, longitude, radiusKm);
        List<ProfessionalDirectoryView> results = new ArrayList<>();
        List<ProfessionalProfile> profiles = professionalRepository.findAll();
        Map<String, double[]> reviewStats = new HashMap<>();
        Map<String, Long> recommendationCounts = new HashMap<>();
        List<String> professionalIds = profiles.stream()
                .map(profile -> profile.getUser().getId())
                .distinct()
                .toList();
        if (!professionalIds.isEmpty()) {
            reviewRepository.summarizeByProfessionalIds(professionalIds).forEach(row ->
                    reviewStats.put((String) row[0],
                            new double[] { ((Number) row[1]).doubleValue(), ((Number) row[2]).longValue() }));
            recommendationRepository.countByProfessionalIds(professionalIds).forEach(row ->
                    recommendationCounts.put((String) row[0], ((Number) row[1]).longValue()));
        }
        for (ProfessionalProfile profile : profiles) {
            User professional = profile.getUser();
            var matchingProfession = professional.getProfessions().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                    .filter(item -> professionId == null || professionId.equals(item.getId()))
                    .sorted(Comparator.comparing(com.hpote.backend.professional.models.Profession::getName))
                    .findFirst()
                    .orElse(null);
            if (!professional.isEnabled()
                    || !"PROFESSIONAL".equalsIgnoreCase(professional.getRole().getName())
                    || !Boolean.TRUE.equals(profile.getIsAvailable())
                    || profile.getLatitude() == null
                    || profile.getLongitude() == null
                    || matchingProfession == null) {
                continue;
            }
            double distance = distanceKm(latitude, longitude, profile.getLatitude(), profile.getLongitude());
            if (distance > radiusKm) continue;
            double[] stats = reviewStats.getOrDefault(professional.getId(), new double[] { 0, 0 });
            results.add(ProfessionalDirectoryView.builder()
                    .id(professional.getId())
                    .name(professional.getName())
                    .profilePhoto(professional.getProfilePhoto())
                    .professionName(matchingProfession.getName())
                    .verified(Boolean.TRUE.equals(profile.getIsVerified()))
                    .rating(stats[1] == 0 ? 0 : stats[0])
                    .reviewCount((long) stats[1])
                    .recommendationCount(recommendationCounts.getOrDefault(professional.getId(), 0L))
                    .distanceLabel(distanceLabel(latitude, longitude,
                            profile.getLatitude(), profile.getLongitude()))
                    .build());
        }
        return results.stream()
                .sorted(Comparator.comparingDouble(ProfessionalDirectoryView::getRating).reversed())
                .limit(RESULTS_LIMIT)
                .toList();
    }

    private RecommendationView toRecommendationView(ProfessionalRecommendation recommendation) {
        User professional = recommendation.getProfessional();
        String professionName = professional.getProfessions().stream()
                .filter(profession -> Boolean.TRUE.equals(profession.getIsActive()))
                .map(com.hpote.backend.professional.models.Profession::getName)
                .sorted()
                .findFirst()
                .orElse("Professional");
        return RecommendationView.builder()
                .professionalId(professional.getId())
                .professionalName(professional.getName())
                .professionName(professionName)
                .profilePhoto(professional.getProfilePhoto())
                .note(recommendation.getNote())
                .createdAt(recommendation.getCreatedAt())
                .build();
    }

    private ServiceRequest findCommunityPost(String postId) {
        return requestRepository.findById(postId)
                .filter(ServiceRequest::isSharedToCommunity)
                .filter(post -> "OPEN".equals(post.getStatus().getName()))
                .orElseThrow(() -> new ApiExceptionDto("Community post not found"));
    }

    private long countLikes(String postId) {
        return likeRepository.countLikesForRequests(List.of(postId)).stream()
                .findFirst()
                .map(row -> ((Number) row[1]).longValue())
                .orElse(0L);
    }

    private void validateSearch(double latitude, double longitude, double radiusKm) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new ApiExceptionDto("A valid search location is required");
        }
        if (!Double.isFinite(radiusKm) || radiusKm < 1 || radiusKm > MAX_RADIUS_KM) {
            throw new ApiExceptionDto("Search radius must be between 1 and 50 km");
        }
    }

    private String distanceLabel(double latitude1, double longitude1, Double latitude2, Double longitude2) {
        double distance = distanceKm(latitude1, longitude1, latitude2, longitude2);
        int roundedDistance = Math.max(1, (int) Math.round(distance));
        return "about " + roundedDistance + " km away";
    }

    private double distanceKm(double latitude1, double longitude1, Double latitude2, Double longitude2) {
        if (latitude2 == null || longitude2 == null) return Double.POSITIVE_INFINITY;
        double deltaLatitude = Math.toRadians(latitude2 - latitude1);
        double deltaLongitude = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2);
        double boundedA = Math.min(1.0, Math.max(0.0, a));
        return 6371.0 * 2 * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA));
    }

    private User findUser(String username) {
        return userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new ApiExceptionDto("Authenticated user not found"));
    }
}
