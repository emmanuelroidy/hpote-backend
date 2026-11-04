package com.hpote.backend.community.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hpote.backend.community.model.ServiceRequestLike;

public interface ServiceRequestLikeRepository extends JpaRepository<ServiceRequestLike, String> {

    boolean existsByServiceRequestIdAndUserId(String requestId, String userId);

    Optional<ServiceRequestLike> findByServiceRequestIdAndUserId(String requestId, String userId);

    List<ServiceRequestLike> findByServiceRequestIdInAndUserId(List<String> requestIds, String userId);

    @Query("select postLike.serviceRequest.id, count(postLike) from ServiceRequestLike postLike "
            + "where postLike.serviceRequest.id in :requestIds group by postLike.serviceRequest.id")
    List<Object[]> countLikesForRequests(@Param("requestIds") List<String> requestIds);
}
