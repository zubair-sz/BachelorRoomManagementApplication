package com.brm.app.dto;

import com.brm.app.entity.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        UUID assignedToId,
        String assignedToName,
        LocalDate dueDate,
        boolean recurring,
        UUID choreId,
        UUID createdById,
        Instant completedAt,
        Instant createdAt
) {}
