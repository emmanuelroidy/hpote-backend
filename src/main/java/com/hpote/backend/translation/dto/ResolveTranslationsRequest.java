package com.hpote.backend.translation.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolveTranslationsRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$") @Size(max = 12) String languageCode,
        @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 2000) String> texts) {
}
