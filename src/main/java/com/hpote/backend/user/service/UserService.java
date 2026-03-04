package com.hpote.backend.user.service;

import com.hpote.backend.common.JwtUtil;
import com.hpote.backend.professional.models.Profession;
import com.hpote.backend.user.dto.UserRegisterRequest;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hpote.backend.user.model.User;
import com.hpote.backend.user.model.UserRole;
import com.hpote.backend.user.repository.UserRepository;
import com.hpote.backend.user.repository.UserRoleRepository;
import com.hpote.backend.professional.repository.ProfessionRepository;
import com.hpote.backend.user.dto.LoginResponse;
import com.hpote.backend.user.dto.LoginRequest;
import com.hpote.backend.common.ApiExceptionDto;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final ProfessionRepository professionRepository;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    // ================= REGISTER =================
    public User registerUser(UserRegisterRequest request) {

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new ApiExceptionDto("Phone already registered");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiExceptionDto("Email already registered");
        }

        Set<Profession> professions = new HashSet<>();

        UserRole role = userRoleRepository
                .findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new ApiExceptionDto("Role not found: " + request.getRole()));

        if ("PROFESSIONAL".equalsIgnoreCase(role.getName())) {

            if (request.getProfessionIds() == null || request.getProfessionIds().isEmpty()) {
                throw new ApiExceptionDto("Professional user must have at least one profession");
            }

            Set<String> professionIdStrings = new HashSet<>();
            for (UUID id : request.getProfessionIds()) {
                professionIdStrings.add(String.valueOf(id));
            }

            professions = new HashSet<>(
                    professionRepository.findAllById(professionIdStrings));

            if (professions.isEmpty()) {
                throw new ApiExceptionDto("Invalid profession IDs");
            }
        }

        User user = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .professions(professions)
                .rating(0.0)
                .build();

        return userRepository.save(user);
    }

    // ================= LOGIN =================
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getUsername())
                .or(() -> userRepository.findByPhone(request.getUsername()))
                .orElseThrow(() ->
                        new ApiExceptionDto("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiExceptionDto("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .userId(UUID.fromString(user.getId()))
                .name(user.getName())
                .role(user.getRole().getName())
                .build();
    }

    // ================= FORGOT PASSWORD =================
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiExceptionDto("User not found with this email"));

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        user.setResetOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        sendOtpEmail(user.getEmail(), otp);
    }

    private void sendOtpEmail(String email, String otp) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("HpOtE Password Reset OTP");
            message.setText(
                    "Hello,\n\n" +
                    "Your OTP for password reset is: " + otp + "\n\n" +
                    "This OTP is valid for 10 minutes.\n\n" +
                    "HpOtE Team"
            );

            mailSender.send(message);

        } catch (Exception e) {
            //throw new ApiExceptionDto("Failed to send OTP email");
            throw new ApiExceptionDto("Failed to send OTP email: " + e.getMessage());
        }
    }

    // ================= RESET PASSWORD =================
    public void resetPassword(String email, String otp, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiExceptionDto("User not found with this email"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new ApiExceptionDto("Invalid OTP");
        }

        if (user.getOtpExpiryTime() == null ||
                user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ApiExceptionDto("OTP expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiryTime(null);

        userRepository.save(user);
    }

    // ================= GET USER =================
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiExceptionDto("User not found with id: " + id));
    }

    // ================= GET ALL USERS =================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

}