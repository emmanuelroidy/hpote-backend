package com.hpote.backend.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ServiceMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
