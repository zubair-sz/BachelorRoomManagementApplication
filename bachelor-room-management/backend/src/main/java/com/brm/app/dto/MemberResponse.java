package com.brm.app.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID userId,
        String name,
        String email,
        boolean admin,
        Instant joinedAt
) {}
