package com.brm.app.dto;

import com.brm.app.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID fromUserId,
        String fromName,
        UUID toUserId,
        String toName,
        BigDecimal amount,
        String note,
        PaymentStatus status,
        Instant createdAt,
        Instant resolvedAt
) {}
