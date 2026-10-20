package com.hpote.backend.admin.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateUserProfessionsRequest(
        @NotEmpty List<@NotNull UUID> professionIds) {
}
