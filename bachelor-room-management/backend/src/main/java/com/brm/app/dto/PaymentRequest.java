package com.brm.app.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code toUserId} defaults to the room admin. */
public record PaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 9, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        UUID toUserId,

        @Size(max = 255, message = "Note must be 255 characters or fewer")
        String note
) {}
