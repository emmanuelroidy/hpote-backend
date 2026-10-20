package com.hpote.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfessionRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        Boolean isActive) {
}
