package com.hpote.backend.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiHelpRequest(
        @NotBlank @Pattern(regexp = "^[A-Z_]{3,40}$") String topic,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$") @Size(max = 12) String languageCode) {
}
