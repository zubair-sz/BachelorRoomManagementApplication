package com.brm.app.controller;

import com.brm.app.dto.AddMemberRequest;
import com.brm.app.dto.CreateRoomRequest;
import com.brm.app.dto.JoinRoomRequest;
import com.brm.app.dto.MemberResponse;
import com.brm.app.dto.RoomResponse;
import com.brm.app.entity.User;
import com.brm.app.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(user, request.name()));
    }

    @GetMapping
    public List<RoomResponse> myRooms(@AuthenticationPrincipal User user) {
        return roomService.myRooms(user);
    }

    @GetMapping("/{roomId}")
    public RoomResponse get(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return roomService.get(roomId, user);
    }

    @PostMapping("/join")
    public RoomResponse join(@Valid @RequestBody JoinRoomRequest request, @AuthenticationPrincipal User user) {
        return roomService.join(user, request.inviteCode());
    }

    @PostMapping("/{roomId}/invite/regenerate")
    public RoomResponse regenerateInvite(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return roomService.regenerateInvite(roomId, user);
    }

    @GetMapping("/{roomId}/members")
    public List<MemberResponse> members(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return roomService.members(roomId, user);
    }

    @PostMapping("/{roomId}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable UUID roomId,
                                                    @Valid @RequestBody AddMemberRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.addByEmail(roomId, user, request.email()));
    }

    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID roomId, @PathVariable UUID userId,
                                             @AuthenticationPrincipal User user) {
        roomService.removeMember(roomId, user, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leave(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        roomService.leave(roomId, user);
        return ResponseEntity.noContent().build();
    }
}
