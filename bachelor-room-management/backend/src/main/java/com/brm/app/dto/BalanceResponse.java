package com.brm.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * net = paid - owed + paymentsSent - paymentsReceived (confirmed payments only).
 * Positive net: the room owes this person. Negative net: this person owes the room.
 */
public record BalanceResponse(
        UUID userId,
        String name,
        BigDecimal paid,
        BigDecimal owed,
        BigDecimal paymentsSent,
        BigDecimal paymentsReceived,
        BigDecimal net
) {}
