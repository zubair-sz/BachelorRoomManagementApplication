package com.brm.app.repository;

import com.brm.app.entity.RecurringChore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringChoreRepository extends JpaRepository<RecurringChore, UUID> {
    List<RecurringChore> findByRoomId(UUID roomId);
    List<RecurringChore> findByActiveTrue();
    Optional<RecurringChore> findByIdAndRoomId(UUID id, UUID roomId);
    List<RecurringChore> findByRoomIdAndAssignedToId(UUID roomId, UUID userId);
}
