package com.brm.app.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode
) {}
