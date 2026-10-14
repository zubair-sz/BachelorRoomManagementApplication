package com.brm.app.dto;

import com.brm.app.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status
) {}
