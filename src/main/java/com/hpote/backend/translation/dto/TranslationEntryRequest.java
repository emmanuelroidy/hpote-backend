package com.hpote.backend.translation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TranslationEntryRequest(
        @NotBlank @Size(max = 160) String translationKey,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$") @Size(max = 12) String sourceLanguage,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*$") @Size(max = 12) String targetLanguage,
        @NotBlank @Size(max = 2000) String sourceText,
        @NotBlank @Size(max = 2000) String translatedText) {
}
