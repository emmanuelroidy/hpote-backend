package com.hpote.backend.messaging.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hpote.backend.common.ApiResponseDto;
import com.hpote.backend.messaging.dto.ServiceMessageRequest;
import com.hpote.backend.messaging.dto.ServiceMessageView;
import com.hpote.backend.messaging.service.ServiceMessagingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/service-requests/{requestId}/messages")
@RequiredArgsConstructor
@Validated
public class ServiceMessagingController {

    private final ServiceMessagingService messagingService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ServiceMessageView>>> getMessages(
            Principal principal, @PathVariable String requestId) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Messages retrieved",
                messagingService.getMessages(principal.getName(), requestId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<ServiceMessageView>> sendMessage(
            Principal principal,
            @PathVariable String requestId,
            @Valid @RequestBody ServiceMessageRequest input) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, 200, "Message sent",
                messagingService.sendMessage(principal.getName(), requestId, input)));
    }
}
