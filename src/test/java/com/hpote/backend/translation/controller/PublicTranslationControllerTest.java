package com.hpote.backend.translation.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.translation.dto.ResolveTranslationsRequest;
import com.hpote.backend.translation.service.TranslationService;

@ExtendWith(MockitoExtension.class)
class PublicTranslationControllerTest {

    @Mock
    private TranslationService translationService;

    @Test
    void resolveEnablesAiFallbackForMissingUiLabels() {
        when(translationService.resolve("sw", List.of("Find help"), true))
                .thenReturn(Map.of("Find help", "Tafuta msaada"));
        PublicTranslationController controller = new PublicTranslationController(translationService);

        ResponseEntity<ApiResponseDto<Map<String, String>>> response = controller.resolve(
                new ResolveTranslationsRequest("sw", List.of("Find help")));

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(translationService).resolve("sw", List.of("Find help"), true);
    }
}
