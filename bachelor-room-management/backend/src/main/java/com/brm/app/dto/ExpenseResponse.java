package com.brm.app.dto;

import com.brm.app.entity.ExpenseSplitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String description,
        BigDecimal amount,
        UUID paidById,
        String paidByName,
        ExpenseSplitType splitType,
        LocalDate date,
        List<ShareResponse> shares,
        Instant createdAt
) {}
