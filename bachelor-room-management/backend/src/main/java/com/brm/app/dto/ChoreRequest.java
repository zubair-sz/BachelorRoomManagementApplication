package com.brm.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * {@code assignedTo} is who gets the first generated task. With {@code rotate} on, the chore then
 * moves to the next roommate each week.
 */
public record ChoreRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be 200 characters or fewer")
        String title,

        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,

        @NotNull(message = "Day of week is required")
        DayOfWeek dayOfWeek,

        UUID assignedTo,

        boolean rotate
) {}
