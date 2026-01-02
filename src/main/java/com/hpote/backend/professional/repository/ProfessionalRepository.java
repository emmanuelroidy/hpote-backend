package com.hpote.backend.professional.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.hpote.backend.professional.models.ProfessionalProfile;


/**
 * Repository for ProfessionalProfile entity
 */
public interface ProfessionalRepository extends JpaRepository<ProfessionalProfile, String> {

    /**
     * Find professional profile by user ID
     * @param userId the user ID
     * @return Optional<ProfessionalProfile>
     */
    Optional<ProfessionalProfile> findByUserId(String userId);

}
