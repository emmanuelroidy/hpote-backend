package com.hpote.backend.service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hpote.backend.service.models.ServiceResponse;

public interface ServiceResponseRepository extends JpaRepository<ServiceResponse, String> {

    boolean existsByServiceRequestIdAndProfessionalProfileId(String requestId, String profileId);

    List<ServiceResponse> findByServiceRequestIdOrderByCreatedAtAsc(String requestId);

    List<ServiceResponse> findByProfessionalProfileIdOrderByCreatedAtDesc(String profileId);

    List<ServiceResponse> findByProfessionalProfileIdAndStatusOrderByCreatedAtDesc(
            String profileId, String status);

    Optional<ServiceResponse> findByIdAndServiceRequestId(String id, String requestId);

    Optional<ServiceResponse> findByServiceRequestIdAndStatus(String requestId, String status);
}
