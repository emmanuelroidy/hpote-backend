package com.hpote.backend.admin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(@NotBlank String role, List<@NotNull UUID> professionIds) {
}
