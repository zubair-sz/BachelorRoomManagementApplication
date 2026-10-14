package com.brm.app.service;

import com.brm.app.dto.PaymentRequest;
import com.brm.app.dto.PaymentResponse;
import com.brm.app.entity.Payment;
import com.brm.app.entity.PaymentStatus;
import com.brm.app.entity.Room;
import com.brm.app.entity.User;
import com.brm.app.exception.ConflictException;
import com.brm.app.exception.ForbiddenException;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.PaymentRepository;
import com.brm.app.util.SplitCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Members log payments they made (to the admin by default); the recipient or the admin confirms
 * the money arrived. Only CONFIRMED payments change balances.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RoomAccessService access;

    /** The admin sees every payment in the room; members see the ones they sent or received. */
    @Transactional(readOnly = true)
    public List<PaymentResponse> list(UUID roomId, User current) {
        Room room = access.requireMember(roomId, current);
        boolean admin = access.isAdmin(room, current.getId());
        return paymentRepository.findByRoomIdOrderByCreatedAtDesc(roomId).stream()
                .filter(p -> admin
                        || p.getFromUser().getId().equals(current.getId())
                        || p.getToUser().getId().equals(current.getId()))
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse log(UUID roomId, User current, PaymentRequest request) {
        Room room = access.requireMember(roomId, current);
        Map<UUID, User> members = access.memberUsers(roomId);

        User recipient = request.toUserId() == null
                ? room.getAdmin()
                : access.requireRoomUser(members, request.toUserId(), "The recipient");
        if (recipient.getId().equals(current.getId())) {
            throw new IllegalArgumentException("You can't log a payment to yourself");
        }

        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        Payment payment = Payment.builder()
                .room(room)
                .fromUser(access.managed(current))
                .toUser(recipient)
                .amount(SplitCalculator.money(request.amount()))
                .note(note)
                .status(PaymentStatus.PENDING)
                .build();
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse confirm(UUID roomId, UUID paymentId, User current) {
        return resolve(roomId, paymentId, current, PaymentStatus.CONFIRMED);
    }

    public PaymentResponse reject(UUID roomId, UUID paymentId, User current) {
        return resolve(roomId, paymentId, current, PaymentStatus.REJECTED);
    }

    /** The payer can withdraw a payment that hasn't been confirmed yet. */
    public void cancel(UUID roomId, UUID paymentId, User current) {
        access.requireMember(roomId, current);
        Payment payment = find(roomId, paymentId);
        if (!payment.getFromUser().getId().equals(current.getId())) {
            throw new ForbiddenException("Only the person who logged a payment can cancel it");
        }
        requirePending(payment);
        paymentRepository.delete(payment);
    }

    private PaymentResponse resolve(UUID roomId, UUID paymentId, User current, PaymentStatus outcome) {
        Room room = access.requireMember(roomId, current);
        Payment payment = find(roomId, paymentId);

        boolean isRecipient = payment.getToUser().getId().equals(current.getId());
        boolean isAdminButNotPayer = access.isAdmin(room, current.getId())
                && !payment.getFromUser().getId().equals(current.getId());
        if (!isRecipient && !isAdminButNotPayer) {
            throw new ForbiddenException("Only the person who received the money, or the admin, can do this");
        }
        requirePending(payment);

        payment.setStatus(outcome);
        payment.setResolvedAt(Instant.now());
        return toResponse(payment);
    }

    private Payment find(UUID roomId, UUID paymentId) {
        return paymentRepository.findByIdAndRoomId(paymentId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    private void requirePending(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ConflictException("This payment has already been " + payment.getStatus().name().toLowerCase(Locale.ROOT));
        }
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getFromUser().getId(),
                p.getFromUser().getName(),
                p.getToUser().getId(),
                p.getToUser().getName(),
                p.getAmount(),
                p.getNote(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getResolvedAt());
    }
}
