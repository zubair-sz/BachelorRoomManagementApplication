package com.brm.app.service;

import com.brm.app.config.AppClock;
import com.brm.app.dto.BalanceResponse;
import com.brm.app.dto.ExpenseRequest;
import com.brm.app.dto.ExpenseResponse;
import com.brm.app.dto.ShareInput;
import com.brm.app.dto.ShareResponse;
import com.brm.app.entity.Expense;
import com.brm.app.entity.ExpenseShare;
import com.brm.app.entity.ExpenseSplitType;
import com.brm.app.entity.Room;
import com.brm.app.entity.User;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.ExpenseRepository;
import com.brm.app.util.SplitCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final RoomAccessService access;
    private final BalanceService balanceService;
    private final AppClock clock;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID roomId, User current) {
        access.requireMember(roomId, current);
        return expenseRepository.findByRoomIdOrderByExpenseDateDescCreatedAtDesc(roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> balances(UUID roomId, User current) {
        access.requireMember(roomId, current);
        return balanceService.balancesForRoom(roomId);
    }

    public ExpenseResponse create(UUID roomId, User current, ExpenseRequest request) {
        Room room = access.requireAdmin(roomId, current);
        Map<UUID, User> members = access.memberUsers(roomId);

        User paidBy = request.paidBy() == null
                ? members.get(current.getId())
                : access.requireRoomUser(members, request.paidBy(), "The person who paid");
        BigDecimal amount = SplitCalculator.money(request.amount());

        Map<UUID, BigDecimal> split = request.splitType() == ExpenseSplitType.EQUAL
                ? equalSplit(amount, request.participantIds(), members)
                : customSplit(amount, request.shares(), members);

        Expense expense = Expense.builder()
                .room(room)
                .paidBy(paidBy)
                .amount(amount)
                .description(request.description().trim())
                .splitType(request.splitType())
                .expenseDate(request.date() == null ? clock.today() : request.date())
                .build();
        split.forEach((userId, share) -> expense.getShares().add(
                ExpenseShare.builder().expense(expense).user(members.get(userId)).shareAmount(share).build()));

        expenseRepository.save(expense);
        return toResponse(expense);
    }

    public void delete(UUID roomId, User current, UUID expenseId) {
        access.requireAdmin(roomId, current);
        Expense expense = expenseRepository.findByIdAndRoomId(expenseId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    private Map<UUID, BigDecimal> equalSplit(BigDecimal amount, List<UUID> participantIds, Map<UUID, User> members) {
        List<UUID> roster = new ArrayList<>(members.keySet());
        List<UUID> chosen;
        if (participantIds == null || participantIds.isEmpty()) {
            chosen = roster;
        } else {
            LinkedHashSet<UUID> unique = new LinkedHashSet<>(participantIds);
            for (UUID id : unique) {
                access.requireRoomUser(members, id, "Everyone in the split");
            }
            // Keep roster order so leftover cents are assigned the same way every time.
            chosen = roster.stream().filter(unique::contains).toList();
        }
        return SplitCalculator.equalSplit(amount, chosen);
    }

    private Map<UUID, BigDecimal> customSplit(BigDecimal amount, List<ShareInput> shares, Map<UUID, User> members) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Enter a share for each person");
        }
        Map<UUID, BigDecimal> raw = new LinkedHashMap<>();
        for (ShareInput share : shares) {
            access.requireRoomUser(members, share.userId(), "Everyone in the split");
            if (raw.put(share.userId(), share.amount()) != null) {
                throw new IllegalArgumentException("Each person can only appear once in the split");
            }
        }
        return SplitCalculator.customSplit(amount, raw);
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ShareResponse> shares = expense.getShares().stream()
                .map(s -> new ShareResponse(s.getUser().getId(), s.getUser().getName(), s.getShareAmount()))
                .toList();
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getSplitType(),
                expense.getExpenseDate(),
                shares,
                expense.getCreatedAt());
    }
}
