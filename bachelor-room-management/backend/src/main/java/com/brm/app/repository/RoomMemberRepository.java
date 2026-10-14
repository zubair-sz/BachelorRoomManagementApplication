package com.brm.app.repository;

import com.brm.app.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {
    List<RoomMember> findByRoomId(UUID roomId);
    List<RoomMember> findByUserId(UUID userId);
    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);
    long countByRoomId(UUID roomId);
}
