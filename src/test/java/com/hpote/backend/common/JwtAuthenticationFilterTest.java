package com.hpote.backend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import com.hpote.backend.user.service.CustomUserDetails;

class JwtAuthenticationFilterTest {

    private final JwtUtil jwtUtil = org.mockito.Mockito.mock(JwtUtil.class);
    private final CustomUserDetails userDetailsService =
            org.mockito.Mockito.mock(CustomUserDetails.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtUtil, userDetailsService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsBearerTokenThatFailsValidation() throws Exception {
        String token = "test-token";
        var userDetails = User.withUsername("person@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(jwtUtil.extractUsername(token)).thenReturn("person@example.com");
        when(userDetailsService.loadUserByUsername("person@example.com"))
                .thenReturn(userDetails);
        when(jwtUtil.isTokenValid(token, userDetails)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/service-requests/mine");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(jwtUtil).isTokenValid(token, userDetails);
        assertNull(chain.getRequest());
    }
}
