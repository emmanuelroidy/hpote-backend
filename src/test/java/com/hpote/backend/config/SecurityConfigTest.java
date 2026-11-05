package com.hpote.backend.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import com.hpote.backend.common.JwtAuthenticationFilter;
import com.hpote.backend.user.service.CustomUserDetails;

class SecurityConfigTest {

    @Test
    void registersJwtFilterOnlyInTheSpringSecurityChain() {
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);
        SecurityConfig config = new SecurityConfig(filter, mock(CustomUserDetails.class));

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                config.jwtFilterServletRegistration();

        assertSame(filter, registration.getFilter());
        assertFalse(registration.isEnabled());
    }
}
