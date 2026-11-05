package com.hpote.backend.assistant;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hpote.backend.assistant.dto.ResolveUiTranslationsRequest;

import jakarta.validation.Validation;

class ResolveUiTranslationsRequestTest {

    @Test
    void acceptsMultilineLabelsForTranslatorFallback() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            ResolveUiTranslationsRequest request =
                    new ResolveUiTranslationsRequest("sw", List.of("Good to have\nyou back."));

            assertTrue(validatorFactory.getValidator().validate(request).isEmpty());
        }
    }
}
