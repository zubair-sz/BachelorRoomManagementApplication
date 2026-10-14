package com.brm.app.repository;

import com.brm.app.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByRoomId(UUID roomId);
    Optional<Task> findByIdAndRoomId(UUID id, UUID roomId);
    List<Task> findByRoomIdAndAssignedToId(UUID roomId, UUID userId);
    List<Task> findByChoreId(UUID choreId);
}
