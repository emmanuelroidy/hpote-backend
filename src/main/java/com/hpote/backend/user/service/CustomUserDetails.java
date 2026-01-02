package com.hpote.backend.user.service;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.hpote.backend.common.ApiExceptionDto;

  @Service
  @RequiredArgsConstructor
    public class CustomUserDetails implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String username)
                throws UsernameNotFoundException {

            User user = userRepository.findByEmail(username)
                    .or(() -> userRepository.findByPhone(username))
                    .orElseThrow(() ->
                      new ApiExceptionDto("User not found"));
                    

            return org.springframework.security.core.userdetails.User
                    .withUsername(username)
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().getName())
                    .build();
        }
    }