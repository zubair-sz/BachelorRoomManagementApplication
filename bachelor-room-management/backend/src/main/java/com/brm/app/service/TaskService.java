package com.brm.app.service;

import com.brm.app.dto.TaskRequest;
import com.brm.app.dto.TaskResponse;
import com.brm.app.entity.Room;
import com.brm.app.entity.Task;
import com.brm.app.entity.TaskStatus;
import com.brm.app.entity.User;
import com.brm.app.exception.ForbiddenException;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskService {

    /** Open tasks first, soonest due date first (undated last), newest first as a tiebreaker. */
    private static final Comparator<Task> ORDER = Comparator
            .comparing((Task t) -> t.getStatus() == TaskStatus.DONE)
            .thenComparing(Task::getDueDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(Task::getCreatedAt, Comparator.<Instant>reverseOrder());

    private final TaskRepository taskRepository;
    private final RoomAccessService access;

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID roomId, User current, boolean mineOnly) {
        access.requireMember(roomId, current);
        return taskRepository.findByRoomId(roomId).stream()
                .filter(t -> !mineOnly || isAssignedTo(t, current.getId()))
                .sorted(ORDER)
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse create(UUID roomId, User current, TaskRequest request) {
        Room room = access.requireAdmin(roomId, current);
        Map<UUID, User> members = access.memberUsers(roomId);

        Task task = Task.builder()
                .room(room)
                .createdBy(members.get(current.getId()))
                .assignedTo(assignee(members, request.assignedTo()))
                .title(request.title().trim())
                .description(blankToNull(request.description()))
                .status(TaskStatus.PENDING)
                .dueDate(request.dueDate())
                .recurring(false)
                .build();
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(UUID roomId, UUID taskId, User current, TaskRequest request) {
        access.requireAdmin(roomId, current);
        Task task = find(roomId, taskId);
        Map<UUID, User> members = access.memberUsers(roomId);

        task.setTitle(request.title().trim());
        task.setDescription(blankToNull(request.description()));
        task.setAssignedTo(assignee(members, request.assignedTo()));
        task.setDueDate(request.dueDate());
        return toResponse(task);
    }

    /** The person a task is assigned to (or the admin) can check it off or reopen it. */
    public TaskResponse setStatus(UUID roomId, UUID taskId, User current, TaskStatus status) {
        Room room = access.requireMember(roomId, current);
        Task task = find(roomId, taskId);

        if (!isAssignedTo(task, current.getId()) && !access.isAdmin(room, current.getId())) {
            throw new ForbiddenException("Only the person this task is assigned to, or the admin, can update it");
        }
        task.setStatus(status);
        task.setCompletedAt(status == TaskStatus.DONE ? Instant.now() : null);
        return toResponse(task);
    }

    public void delete(UUID roomId, UUID taskId, User current) {
        access.requireAdmin(roomId, current);
        taskRepository.delete(find(roomId, taskId));
    }

    private Task find(UUID roomId, UUID taskId) {
        return taskRepository.findByIdAndRoomId(taskId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private User assignee(Map<UUID, User> members, UUID assigneeId) {
        return assigneeId == null ? null : access.requireRoomUser(members, assigneeId, "The assignee");
    }

    private boolean isAssignedTo(Task task, UUID userId) {
        return task.getAssignedTo() != null && task.getAssignedTo().getId().equals(userId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TaskResponse toResponse(Task t) {
        User assignee = t.getAssignedTo();
        return new TaskResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName(),
                t.getDueDate(),
                t.isRecurring(),
                t.getChore() == null ? null : t.getChore().getId(),
                t.getCreatedBy().getId(),
                t.getCompletedAt(),
                t.getCreatedAt());
    }
}
