package com.hpote.backend.service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceResponse {

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal quoteAmount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}", message = "Currency code must be a three-letter ISO code")
    private String currencyCode;

    @NotBlank
    @Size(max = 1000)
    private String message;

    @Positive
    private Integer estimatedDays;
}
