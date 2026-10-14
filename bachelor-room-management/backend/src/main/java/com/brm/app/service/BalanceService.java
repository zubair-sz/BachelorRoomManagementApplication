package com.brm.app.service;

import com.brm.app.dto.BalanceResponse;
import com.brm.app.entity.Payment;
import com.brm.app.entity.PaymentStatus;
import com.brm.app.entity.RoomMember;
import com.brm.app.repository.ExpenseRepository;
import com.brm.app.repository.ExpenseShareRepository;
import com.brm.app.repository.PaymentRepository;
import com.brm.app.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ledger maths. For each member:
 * <pre>net = paid - owed + paymentsSent - paymentsReceived</pre>
 * where only CONFIRMED payments count. The nets of all members always add up to zero.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository shareRepository;
    private final PaymentRepository paymentRepository;
    private final RoomMemberRepository memberRepository;

    public List<BalanceResponse> balancesForRoom(UUID roomId) {
        Map<UUID, BigDecimal> paid = new HashMap<>();
        Map<UUID, BigDecimal> owed = new HashMap<>();
        Map<UUID, BigDecimal> sent = new HashMap<>();
        Map<UUID, BigDecimal> received = new HashMap<>();

        expenseRepository.findByRoomId(roomId)
                .forEach(e -> paid.merge(e.getPaidBy().getId(), e.getAmount(), BigDecimal::add));
        shareRepository.findByExpense_Room_Id(roomId)
                .forEach(s -> owed.merge(s.getUser().getId(), s.getShareAmount(), BigDecimal::add));
        for (Payment p : paymentRepository.findByRoomIdAndStatus(roomId, PaymentStatus.CONFIRMED)) {
            sent.merge(p.getFromUser().getId(), p.getAmount(), BigDecimal::add);
            received.merge(p.getToUser().getId(), p.getAmount(), BigDecimal::add);
        }

        return memberRepository.findByRoomId(roomId).stream()
                .sorted(Comparator.comparing(RoomMember::getJoinedAt))
                .map(member -> {
                    UUID id = member.getUser().getId();
                    BigDecimal p = paid.getOrDefault(id, ZERO);
                    BigDecimal o = owed.getOrDefault(id, ZERO);
                    BigDecimal s = sent.getOrDefault(id, ZERO);
                    BigDecimal r = received.getOrDefault(id, ZERO);
                    BigDecimal net = p.subtract(o).add(s).subtract(r);
                    return new BalanceResponse(id, member.getUser().getName(), p, o, s, r, net);
                })
                .toList();
    }

    public BigDecimal netFor(UUID roomId, UUID userId) {
        return balancesForRoom(roomId).stream()
                .filter(b -> b.userId().equals(userId))
                .map(BalanceResponse::net)
                .findFirst()
                .orElse(ZERO);
    }
}
