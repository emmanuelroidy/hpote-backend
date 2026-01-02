package com.hpote.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.hpote.backend.user.model.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    Optional<UserRole> findByName(String name);
}
