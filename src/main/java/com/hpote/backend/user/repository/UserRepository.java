package com.hpote.backend.user.repository;

import com.hpote.backend.user.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for User entity
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find a user by phone number
     * @param phone the phone number
     * @return Optional<User>
     */
    Optional<User> findByPhone(String phone);

    /**
     * Find a user by email
     * @param email the email address
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    long countByRole_Name(String roleName);

    long countByRole_NameAndEnabled(String roleName, boolean enabled);

    long countByRole_NameAndIdNot(String roleName, String id);

    long countByEnabled(boolean enabled);

    boolean existsByProfessions_Id(String professionId);
}
