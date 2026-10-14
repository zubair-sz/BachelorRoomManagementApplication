package com.brm.app.repository;

import com.brm.app.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByRoomId(UUID roomId);
    List<Expense> findByRoomIdOrderByExpenseDateDescCreatedAtDesc(UUID roomId);
    Optional<Expense> findByIdAndRoomId(UUID id, UUID roomId);
}
