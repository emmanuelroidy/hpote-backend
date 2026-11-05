package com.hpote.backend.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.hpote.backend.user.service.CustomUserDetails;

import java.io.IOException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CustomUserDetails userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip JWT processing for public endpoints
        if (path.startsWith("/api/users/login") ||
            path.startsWith("/api/users/register") ||
            path.startsWith("/api/users/forgot-password") ||
            path.startsWith("/api/users/reset-password") || 
            path.startsWith("/api/public/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT missing for protected request {}", path);
            // No token provided, continue as anonymous
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("JWT rejected for {}: token could not be parsed", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (username == null) {
            log.warn("JWT rejected for {}: token has no subject", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException exception) {
                log.warn("JWT rejected for {}: subject does not match an account", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            boolean valid;
            try {
                valid = jwtUtil.isTokenValid(token, userDetails);
            } catch (JwtException | IllegalArgumentException exception) {
                log.warn("JWT rejected for {}: token validation failed", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!valid) {
                log.warn("JWT rejected for {}: token is expired or subject does not match", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (!userDetails.isEnabled()) {
                log.warn("JWT rejected for {}: account is disabled", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}