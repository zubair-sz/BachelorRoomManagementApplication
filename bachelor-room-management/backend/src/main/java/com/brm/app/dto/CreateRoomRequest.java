package com.brm.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "Room name is required")
        @Size(max = 150, message = "Room name must be 150 characters or fewer")
        String name
) {}
