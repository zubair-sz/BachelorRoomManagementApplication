package com.brm.app.service;

import com.brm.app.dto.MemberResponse;
import com.brm.app.dto.RoomResponse;
import com.brm.app.entity.PaymentStatus;
import com.brm.app.entity.Role;
import com.brm.app.entity.Room;
import com.brm.app.entity.RoomMember;
import com.brm.app.entity.TaskStatus;
import com.brm.app.entity.User;
import com.brm.app.exception.ConflictException;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.PaymentRepository;
import com.brm.app.repository.RecurringChoreRepository;
import com.brm.app.repository.RoomMemberRepository;
import com.brm.app.repository.RoomRepository;
import com.brm.app.repository.TaskRepository;
import com.brm.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RecurringChoreRepository choreRepository;
    private final PaymentRepository paymentRepository;
    private final RoomAccessService access;
    private final BalanceService balanceService;

    public RoomResponse create(User current, String name) {
        User creator = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Room room = roomRepository.save(Room.builder().name(name.trim()).admin(creator).build());
        memberRepository.save(RoomMember.builder().room(room).user(creator).build());

        // Creating a room makes you an admin (of that room). Authorization itself is checked
        // per room via Room.admin, so being admin here doesn't grant anything in other rooms.
        if (creator.getRole() != Role.ROOM_ADMIN) {
            creator.setRole(Role.ROOM_ADMIN);
            userRepository.save(creator);
        }
        return toResponse(room, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> myRooms(User current) {
        return memberRepository.findByUserId(current.getId()).stream()
                .map(RoomMember::getRoom)
                .sorted(Comparator.comparing(Room::getCreatedAt))
                .map(room -> toResponse(room, current.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(UUID roomId, User current) {
        Room room = access.requireMember(roomId, current);
        return toResponse(room, current.getId());
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> members(UUID roomId, User current) {
        Room room = access.requireMember(roomId, current);
        return access.members(roomId).stream()
                .map(m -> toMemberResponse(m, room))
                .toList();
    }

    public MemberResponse addByEmail(UUID roomId, User current, String email) {
        Room room = access.requireAdmin(roomId, current);
        User user = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No registered user has that email. Ask them to sign up, or share the invite link instead."));
        if (memberRepository.findByRoomIdAndUserId(roomId, user.getId()).isPresent()) {
            throw new ConflictException(user.getName() + " is already in this room");
        }
        RoomMember member = memberRepository.save(RoomMember.builder().room(room).user(user).build());
        return toMemberResponse(member, room);
    }

    /** Joining with a code you are already in is a harmless no-op, so invite links can be opened twice. */
    public RoomResponse join(User current, String inviteCode) {
        Room room = roomRepository.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("That invite code isn't valid"));
        if (memberRepository.findByRoomIdAndUserId(room.getId(), current.getId()).isEmpty()) {
            memberRepository.save(RoomMember.builder().room(room).user(access.managed(current)).build());
        }
        return toResponse(room, current.getId());
    }

    public RoomResponse regenerateInvite(UUID roomId, User current) {
        Room room = access.requireAdmin(roomId, current);
        room.setInviteCode(Room.generateInviteCode());
        return toResponse(room, current.getId());
    }

    public void removeMember(UUID roomId, User current, UUID userId) {
        Room room = access.requireAdmin(roomId, current);
        if (access.isAdmin(room, userId)) {
            throw new ConflictException("The room admin can't be removed");
        }
        detach(room, userId);
    }

    public void leave(UUID roomId, User current) {
        Room room = access.requireMember(roomId, current);
        if (access.isAdmin(room, current.getId())) {
            throw new ConflictException("The room admin can't leave the room");
        }
        detach(room, current.getId());
    }

    private void detach(Room room, UUID userId) {
        UUID roomId = room.getId();
        RoomMember membership = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("That person is not in this room"));

        BigDecimal net = balanceService.netFor(roomId, userId);
        if (net.signum() != 0) {
            String direction = net.signum() < 0 ? "owes" : "is owed";
            throw new ConflictException(membership.getUser().getName() + " " + direction + " "
                    + net.abs().toPlainString() + ". Settle up before they leave the room.");
        }

        // A payment still waiting for confirmation would move balances after the person has gone.
        boolean hasPendingPayment = paymentRepository.findByRoomIdAndStatus(roomId, PaymentStatus.PENDING).stream()
                .anyMatch(p -> p.getFromUser().getId().equals(userId) || p.getToUser().getId().equals(userId));
        if (hasPendingPayment) {
            throw new ConflictException(membership.getUser().getName()
                    + " has a payment waiting for confirmation. Confirm or reject it first.");
        }

        // Open work shouldn't stay assigned to someone who is no longer here.
        taskRepository.findByRoomIdAndAssignedToId(roomId, userId).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .forEach(t -> t.setAssignedTo(null));
        choreRepository.findByRoomIdAndAssignedToId(roomId, userId)
                .forEach(c -> c.setAssignedTo(null));

        memberRepository.delete(membership);
    }

    private RoomResponse toResponse(Room room, UUID viewerId) {
        boolean admin = access.isAdmin(room, viewerId);
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getAdmin().getId(),
                room.getAdmin().getName(),
                admin ? room.getInviteCode() : null,
                memberRepository.countByRoomId(room.getId()),
                admin);
    }

    private MemberResponse toMemberResponse(RoomMember member, Room room) {
        User user = member.getUser();
        return new MemberResponse(user.getId(), user.getName(), user.getEmail(),
                access.isAdmin(room, user.getId()), member.getJoinedAt());
    }
}
