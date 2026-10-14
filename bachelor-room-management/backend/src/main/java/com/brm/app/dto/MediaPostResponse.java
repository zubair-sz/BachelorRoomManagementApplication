package com.brm.app.dto;

import com.brm.app.entity.MediaKind;

import java.time.Instant;
import java.util.UUID;

/** {@code fileUrl} is an API path (relative to /api) that needs the caller's JWT, or null for text posts. */
public record MediaPostResponse(
        UUID id,
        UUID userId,
        String userName,
        String caption,
        String fileUrl,
        MediaKind fileType,
        String contentType,
        Instant postedAt
) {}
