package com.hpote.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;
import com.hpote.backend.user.repository.UserRoleRepository;

class AdminBootstrapRunnerTest {

    private Environment environment;
    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private PasswordEncoder passwordEncoder;
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        runner = new AdminBootstrapRunner(
                environment, userRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    void createsConfiguredAdminWithEncodedPassword() {
        configure("Emmanuel Roidy", "Admin@Example.test", "0742901155",
                "a-strong-bootstrap-password");
        UserRole adminRole = UserRole.builder().name("ADMIN").build();
        when(userRepository.findByEmail("admin@example.test")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0742901155")).thenReturn(Optional.empty());
        when(userRoleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("a-strong-bootstrap-password")).thenReturn("encoded-password");

        runner.run(mock(ApplicationArguments.class));

        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("Emmanuel Roidy", saved.getValue().getName());
        assertEquals("admin@example.test", saved.getValue().getEmail());
        assertEquals("0742901155", saved.getValue().getPhone());
        assertEquals("encoded-password", saved.getValue().getPassword());
        assertEquals(adminRole, saved.getValue().getRole());
        verify(passwordEncoder).encode("a-strong-bootstrap-password");
    }

    @Test
    void leavesExistingAdminAccountUnchanged() {
        configure("Emmanuel Roidy", "admin@example.test", "0742901155",
                "a-strong-bootstrap-password");
        when(userRepository.findByEmail("admin@example.test"))
                .thenReturn(Optional.of(User.builder()
                        .role(UserRole.builder().name("ADMIN").build())
                        .build()));

        runner.run(mock(ApplicationArguments.class));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void leavesExistingNonAdminAccountUnchanged() {
        configure("Emmanuel Roidy", "admin@example.test", "0742901155",
                "a-strong-bootstrap-password");
        when(userRepository.findByEmail("admin@example.test"))
                .thenReturn(Optional.of(User.builder()
                        .role(UserRole.builder().name("NORMAL").build())
                        .build()));

        runner.run(mock(ApplicationArguments.class));
        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotCreateAdminWithWeakBootstrapPassword() {
        configure("Emmanuel Roidy", "admin@example.test", "0742901155", "Admin@2026");

        runner.run(mock(ApplicationArguments.class));
        verify(userRepository, never()).save(any());
    }

    private void configure(String name, String email, String phone, String password) {
        when(environment.getProperty("BOOTSTRAP_ADMIN_NAME", "")).thenReturn(name);
        when(environment.getProperty("BOOTSTRAP_ADMIN_EMAIL", "")).thenReturn(email);
        when(environment.getProperty("BOOTSTRAP_ADMIN_PHONE", "")).thenReturn(phone);
        when(environment.getProperty("BOOTSTRAP_ADMIN_PASSWORD", "")).thenReturn(password);
    }
}
