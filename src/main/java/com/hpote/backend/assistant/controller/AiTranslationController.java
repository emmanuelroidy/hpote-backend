package com.hpote.backend.assistant.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.assistant.dto.ResolveUiTranslationsRequest;
import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.translation.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assistant/translations")
@RequiredArgsConstructor
@Validated
public class AiTranslationController {

    private final TranslationService translationService;

    @PostMapping("/resolve")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> resolve(
            @Valid @RequestBody ResolveUiTranslationsRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(
                translationService.resolve(request.languageCode(), request.texts(), true)));
    }
}
