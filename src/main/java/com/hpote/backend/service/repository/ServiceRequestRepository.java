package com.hpote.backend.service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hpote.backend.service.models.ServiceRequest;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(String requesterId);

    List<ServiceRequest> findByStatus_NameAndProfession_IdOrderByCreatedAtDesc(
            String status, String professionId);

    Optional<ServiceRequest> findByIdAndRequesterId(String id, String requesterId);
}
