package com.brm.app.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/** {@code assignedToId} is who is next up for this chore. */
public record ChoreResponse(
        UUID id,
        String title,
        String description,
        DayOfWeek dayOfWeek,
        UUID assignedToId,
        String assignedToName,
        boolean rotate,
        boolean active,
        LocalDate lastGeneratedDate
) {}
