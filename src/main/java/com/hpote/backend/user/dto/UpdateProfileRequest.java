package com.hpote.backend.user.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String profilePhoto,
        List<UUID> professionIds) {
}
