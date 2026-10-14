package com.brm.app.dto;

import jakarta.validation.constraints.NotNull;

public record ChoreActiveRequest(
        @NotNull(message = "Active flag is required")
        Boolean active
) {}
