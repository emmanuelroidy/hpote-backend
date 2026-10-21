package com.hpote.backend.admin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 40) String phone,
        @Email @Size(max = 255) String email,
        @NotBlank String role,
        List<@NotNull UUID> professionIds) {
}
