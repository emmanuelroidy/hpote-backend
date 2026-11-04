package com.hpote.backend.messaging.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hpote.backend.messaging.model.ServiceMessage;

public interface ServiceMessageRepository extends JpaRepository<ServiceMessage, String> {

    List<ServiceMessage> findTop100ByServiceRequestIdOrderByCreatedAtDesc(String serviceRequestId);
}
