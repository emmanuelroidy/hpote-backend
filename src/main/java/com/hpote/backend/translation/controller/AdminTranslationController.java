package com.hpote.backend.translation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.translation.dto.TranslationEntryRequest;
import com.hpote.backend.translation.model.TranslationEntry;
import com.hpote.backend.translation.service.TranslationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/translations")
@RequiredArgsConstructor
@Validated
public class AdminTranslationController {

    private final TranslationService translationService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<TranslationEntry>>> entries(
            @RequestParam(defaultValue = "sw") String languageCode) {
        return ResponseEntity.ok(ApiResponseDto.success(translationService.entries(languageCode)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<TranslationEntry>> create(
            @Valid @RequestBody TranslationEntryRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(translationService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<TranslationEntry>> update(
            @PathVariable String id,
            @Valid @RequestBody TranslationEntryRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(translationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(@PathVariable String id) {
        translationService.delete(id);
        return ResponseEntity.ok(ApiResponseDto.success(null));
    }
}
