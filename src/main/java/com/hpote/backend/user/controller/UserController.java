package com.hpote.backend.user.controller;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.user.dto.ForgotPasswordRequest;
import com.hpote.backend.user.dto.LoginRequest;
import com.hpote.backend.user.dto.LoginResponse;
import com.hpote.backend.user.dto.ResetPasswordRequest;
import com.hpote.backend.user.dto.UserRegisterRequest;
import com.hpote.backend.user.dto.UserView;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.hpote.backend.user.model.User;
import com.hpote.backend.user.service.UserService;

import jakarta.annotation.security.PermitAll;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Validated @RequestBody UserRegisterRequest request) {
        User user = userService.registerUser(request);
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User registered successfully",
                UserView.from(user)));
    }

    @PostMapping("/login")
    @PermitAll
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(new ApiResponseDto<LoginResponse>(true, 200, "Login successful", response));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<UserView>> getUserById(
            @PathVariable String id, Authentication authentication) {
        User user = userService.getUserById(id);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!isAdmin && !authentication.getName().equals(user.getEmail())
                && !authentication.getName().equals(user.getPhone())) {
            throw new AccessDeniedException("You can only view your own account");
        }
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "User retrieved successfully",
                UserView.from(user)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<List<UserView>>> getAllUsers() {
        List<UserView> users = userService.getAllUsers().stream()
                .map(UserView::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Users retrieved successfully", users));
    }

    @PostMapping("/forgot-password")
    @PermitAll
    public ResponseEntity<ApiResponseDto<Object>> forgotPassword(
            @Validated @RequestBody ForgotPasswordRequest request) {

        userService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                new ApiResponseDto<>(true, 200, "If the account exists, a reset code has been sent", null));
    }

    @PostMapping("/reset-password")
    @PermitAll
    public ResponseEntity<ApiResponseDto<Object>> resetPassword(
            @Validated @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword());

        return ResponseEntity.ok(
                new ApiResponseDto<>(true, 200, "Password reset successful", null));
    }

}
