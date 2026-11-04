package com.hpote.backend.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecommendProfessionalRequest {

    @NotBlank
    private String professionalId;

    @Size(max = 240)
    private String note;
}
