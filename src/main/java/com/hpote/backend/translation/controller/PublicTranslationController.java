package com.hpote.backend.translation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.translation.dto.ResolveTranslationsRequest;
import com.hpote.backend.translation.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/translations")
@RequiredArgsConstructor
@Validated
public class PublicTranslationController {

    private final TranslationService translationService;

    @GetMapping("/languages")
    public ResponseEntity<ApiResponseDto<List<Map<String, String>>>> languages() {
        return ResponseEntity.ok(ApiResponseDto.success(translationService.languages()));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<Map<String, String>>> dictionary(
            @RequestParam(defaultValue = "en") String languageCode) {
        return ResponseEntity.ok(ApiResponseDto.success(translationService.dictionary(languageCode)));
    }

    @PostMapping("/resolve")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> resolve(
            @Valid @RequestBody ResolveTranslationsRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(
                translationService.resolve(request.languageCode(), request.texts())));
    }
}
