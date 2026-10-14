package com.brm.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareInput(
        @NotNull(message = "Person is required")
        UUID userId,

        @NotNull(message = "Share amount is required")
        @DecimalMin(value = "0.00", message = "Shares can't be negative")
        @Digits(integer = 9, fraction = 2, message = "Share must have at most 2 decimal places")
        BigDecimal amount
) {}
