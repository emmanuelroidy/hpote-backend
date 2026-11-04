package com.hpote.backend.assistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.assistant.dto.AiHelpRequest;
import com.hpote.backend.assistant.dto.AiHelpView;
import com.hpote.backend.assistant.service.AiHelpService;
import com.hpote.backend.common.ApiResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Validated
public class AiHelpController {

    private final AiHelpService aiHelpService;

    @PostMapping("/help")
    public ResponseEntity<ApiResponseDto<AiHelpView>> help(
            @Valid @RequestBody AiHelpRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(
                aiHelpService.answer(request.topic(), request.languageCode())));
    }
}
