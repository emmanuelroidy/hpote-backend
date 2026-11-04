package com.hpote.backend.reviews.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hpote.backend.reviews.model.ServiceReview;

public interface ServiceReviewRepository extends JpaRepository<ServiceReview, String> {

    Optional<ServiceReview> findByServiceRequestId(String requestId);

    boolean existsByServiceRequestId(String requestId);

    List<ServiceReview> findTop20ByProfessionalIdOrderByCreatedAtDesc(String professionalId);

    @Query("select review.professional.id, avg(review.rating), count(review) from ServiceReview review "
            + "where review.professional.id in :professionalIds group by review.professional.id")
    List<Object[]> summarizeByProfessionalIds(@Param("professionalIds") List<String> professionalIds);
}
