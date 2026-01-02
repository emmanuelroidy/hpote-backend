package com.hpote.backend.professional.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.hpote.backend.professional.models.Profession;

public interface ProfessionRepository extends JpaRepository<Profession, String> {
    Optional<Profession> findByName(String name);
}
