package com.brm.app.dto;

import java.util.UUID;

/**
 * {@code inviteCode} is only filled in for the room admin; {@code admin} says whether the
 * caller is the admin of this particular room.
 */
public record RoomResponse(
        UUID id,
        String name,
        UUID adminId,
        String adminName,
        String inviteCode,
        long memberCount,
        boolean admin
) {}
