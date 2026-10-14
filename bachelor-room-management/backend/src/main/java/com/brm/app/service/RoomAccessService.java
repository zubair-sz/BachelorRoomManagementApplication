package com.brm.app.service;

import com.brm.app.entity.Room;
import com.brm.app.entity.RoomMember;
import com.brm.app.entity.User;
import com.brm.app.exception.ForbiddenException;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.RoomMemberRepository;
import com.brm.app.repository.RoomRepository;
import com.brm.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central place for "is this person allowed here?" checks. Admin rights are per room: the admin
 * of one room is just a member of any other room they join.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomAccessService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final UserRepository userRepository;

    public Room requireMember(UUID roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (memberRepository.findByRoomIdAndUserId(roomId, user.getId()).isEmpty()) {
            throw new ForbiddenException("You are not a member of this room");
        }
        return room;
    }

    public Room requireAdmin(UUID roomId, User user) {
        Room room = requireMember(roomId, user);
        if (!isAdmin(room, user.getId())) {
            throw new ForbiddenException("Only the room admin can do this");
        }
        return room;
    }

    public boolean isAdmin(Room room, UUID userId) {
        return room.getAdmin().getId().equals(userId);
    }

    /** Members in a stable order (join date), which also fixes who gets leftover cents and rotation order. */
    public List<RoomMember> members(UUID roomId) {
        return memberRepository.findByRoomId(roomId).stream()
                .sorted(Comparator.comparing(RoomMember::getJoinedAt)
                        .thenComparing(m -> m.getUser().getId()))
                .toList();
    }

    public Map<UUID, User> memberUsers(UUID roomId) {
        Map<UUID, User> result = new LinkedHashMap<>();
        for (RoomMember member : members(roomId)) {
            result.put(member.getUser().getId(), member.getUser());
        }
        return result;
    }

    public User requireRoomUser(Map<UUID, User> members, UUID userId, String who) {
        User found = members.get(userId);
        if (found == null) {
            throw new IllegalArgumentException(who + " must be a member of this room");
        }
        return found;
    }

    /** The logged-in principal comes from a previous request; this gives a reference tied to the current transaction. */
    public User managed(User principal) {
        return userRepository.getReferenceById(principal.getId());
    }
}
