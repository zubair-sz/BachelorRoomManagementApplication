package com.brm.app.dto;

import com.brm.app.entity.ExpenseSplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * {@code paidBy} defaults to the admin; {@code date} defaults to today.
 * EQUAL uses {@code participantIds} (empty = everyone); CUSTOM uses {@code shares}.
 */
public record ExpenseRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be 255 characters or fewer")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 9, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        UUID paidBy,

        @NotNull(message = "Split type is required")
        ExpenseSplitType splitType,

        LocalDate date,

        List<UUID> participantIds,

        @Valid
        List<ShareInput> shares
) {}
