package com.hpote.backend.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.hpote.backend.service.models.ServiceStatus;

public interface ServiceStatusRepository extends JpaRepository<ServiceStatus, String> {
    Optional<ServiceStatus> findByName(String name);
}
