package com.brm.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareResponse(
        UUID userId,
        String name,
        BigDecimal amount
) {}
