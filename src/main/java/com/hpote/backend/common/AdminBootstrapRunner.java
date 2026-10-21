package com.hpote.backend.common;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;
import com.hpote.backend.user.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String NAME = "BOOTSTRAP_ADMIN_NAME";
    private static final String EMAIL = "BOOTSTRAP_ADMIN_EMAIL";
    private static final String PHONE = "BOOTSTRAP_ADMIN_PHONE";
    private static final String PASSWORD = "BOOTSTRAP_ADMIN_PASSWORD";
    private static final int MIN_PASSWORD_BYTES = 16;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final Environment environment;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String name = value(NAME);
        String email = value(EMAIL).toLowerCase(Locale.ROOT);
        String phone = value(PHONE);
        String password = value(PASSWORD);

        if (name.isEmpty() && email.isEmpty() && phone.isEmpty() && password.isEmpty()) {
            return;
        }
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            log.error(
                    "Configure BOOTSTRAP_ADMIN_NAME, BOOTSTRAP_ADMIN_EMAIL, "
                            + "BOOTSTRAP_ADMIN_PHONE, and BOOTSTRAP_ADMIN_PASSWORD together.");
            return;
        }

        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < MIN_PASSWORD_BYTES || passwordBytes > MAX_BCRYPT_PASSWORD_BYTES) {
            log.error(
                    "BOOTSTRAP_ADMIN_PASSWORD must be 16 to 72 UTF-8 bytes.");
            return;
        }

        var existingEmail = userRepository.findByEmail(email);
        if (existingEmail.isPresent()) {
            if (existingEmail.get().getRole() == null
                    || !"ADMIN".equals(existingEmail.get().getRole().getName())) {
                log.error(
                        "The bootstrap email already belongs to a non-admin account. "
                                + "No account was changed; use the admin portal to change its role.");
                return;
            }
            log.info("Bootstrap administrator already exists; leaving its account unchanged.");
            return;
        }
        if (userRepository.findByPhone(phone).isPresent()) {
            log.error("The bootstrap phone already belongs to another account. No account was created.");
            return;
        }

        UserRole adminRole = userRoleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role was not initialized before admin bootstrap."));
        User administrator = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(password))
                .role(adminRole)
                .enabled(true)
                .professions(new HashSet<>())
                .build();
        userRepository.save(administrator);
        log.info("Created the configured bootstrap administrator.");
    }

    private String value(String key) {
        return environment.getProperty(key, "").trim();
    }
}
