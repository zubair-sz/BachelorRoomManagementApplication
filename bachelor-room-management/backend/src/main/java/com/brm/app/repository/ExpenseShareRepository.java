package com.brm.app.repository;

import com.brm.app.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, UUID> {
    List<ExpenseShare> findByExpense_Room_Id(UUID roomId);
}
