package com.hpote.backend.user.controller;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.professional.models.ProfessionalProfile;
import com.hpote.backend.user.dto.ForgotPasswordRequest;
import com.hpote.backend.user.dto.LoginRequest;
import com.hpote.backend.user.dto.LoginResponse;
import com.hpote.backend.user.dto.ResetPasswordRequest;
import com.hpote.backend.user.dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
        try {
            User user = userService.registerUser(request);

            return ResponseEntity.ok(new ApiResponseDto<User>(true, 200, "User registered successfully", user));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ApiResponseDto<Object>(false, 400, ex.getMessage(), null));
        }
    }

    @PostMapping("/login")
    @PermitAll
    public ResponseEntity<ApiResponseDto<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(new ApiResponseDto<LoginResponse>(true, 200, "Login successful", response));

    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<User>> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponseDto<User>(true, 200, "User retrieved successfully", user));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDto<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponseDto<List<User>>(true, 200, "Users retrieved successfully", users));
    }

    @PostMapping("/forgot-password")
    @PermitAll
    public ResponseEntity<ApiResponseDto<Object>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        userService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                new ApiResponseDto<>(true, 200, "OTP sent to email", null));
    }

    @PostMapping("/reset-password")
    @PermitAll
    public ResponseEntity<ApiResponseDto<Object>> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        userService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword());

        return ResponseEntity.ok(
                new ApiResponseDto<>(true, 200, "Password reset successful", null));
    }

}
