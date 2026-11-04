package com.hpote.backend.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hpote.backend.community.model.ProfessionalRecommendation;

public interface ProfessionalRecommendationRepository
        extends JpaRepository<ProfessionalRecommendation, String> {

    boolean existsByServiceRequestIdAndRecommenderIdAndProfessionalId(
            String requestId, String recommenderId, String professionalId);

    Optional<ProfessionalRecommendation> findByServiceRequestIdAndRecommenderIdAndProfessionalId(
            String requestId, String recommenderId, String professionalId);

    List<ProfessionalRecommendation> findByServiceRequestIdInOrderByCreatedAtDesc(
            List<String> requestIds);

    @Query("select recommendation.professional.id, count(recommendation) "
            + "from ProfessionalRecommendation recommendation "
            + "where recommendation.professional.id in :professionalIds "
            + "group by recommendation.professional.id")
    List<Object[]> countByProfessionalIds(@Param("professionalIds") List<String> professionalIds);
}
