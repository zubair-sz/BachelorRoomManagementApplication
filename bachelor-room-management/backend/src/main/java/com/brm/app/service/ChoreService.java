package com.brm.app.service;

import com.brm.app.config.AppClock;
import com.brm.app.dto.ChoreRequest;
import com.brm.app.dto.ChoreResponse;
import com.brm.app.entity.RecurringChore;
import com.brm.app.entity.Room;
import com.brm.app.entity.User;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.RecurringChoreRepository;
import com.brm.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Weekly chores. {@link #generateDueTasks} is called by the midnight scheduler (ChoreScheduler)
 * and is safe to run repeatedly; the work for each chore happens in {@link ChoreGenerator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChoreService {

    private final RecurringChoreRepository choreRepository;
    private final TaskRepository taskRepository;
    private final RoomAccessService access;
    private final ChoreGenerator generator;
    private final AppClock clock;

    @Transactional(readOnly = true)
    public List<ChoreResponse> list(UUID roomId, User current) {
        access.requireMember(roomId, current);
        return choreRepository.findByRoomId(roomId).stream()
                .sorted(Comparator.comparing(RecurringChore::getDayOfWeek)
                        .thenComparing(RecurringChore::getTitle))
                .map(this::toResponse)
                .toList();
    }

    public ChoreResponse create(UUID roomId, User current, ChoreRequest request) {
        Room room = access.requireAdmin(roomId, current);
        Map<UUID, User> members = access.memberUsers(roomId);

        User nextUp = request.assignedTo() == null
                ? null
                : access.requireRoomUser(members, request.assignedTo(), "The assignee");
        if (nextUp == null && request.rotate() && !members.isEmpty()) {
            nextUp = members.values().iterator().next(); // rotating chores always start with someone
        }

        RecurringChore chore = choreRepository.save(RecurringChore.builder()
                .room(room)
                .createdBy(members.get(current.getId()))
                .title(request.title().trim())
                .description(request.description() == null || request.description().isBlank()
                        ? null : request.description().trim())
                .dayOfWeek(request.dayOfWeek())
                .assignedTo(nextUp)
                .rotate(request.rotate())
                .active(true)
                .build());

        // A chore created on its own weekday should show up straight away rather than next week.
        LocalDate today = clock.today();
        if (chore.getDayOfWeek() == today.getDayOfWeek()) {
            generator.generateFor(chore, today);
        }
        return toResponse(chore);
    }

    public ChoreResponse setActive(UUID roomId, UUID choreId, User current, boolean active) {
        access.requireAdmin(roomId, current);
        RecurringChore chore = find(roomId, choreId);
        chore.setActive(active);
        return toResponse(chore);
    }

    public void delete(UUID roomId, UUID choreId, User current) {
        access.requireAdmin(roomId, current);
        RecurringChore chore = find(roomId, choreId);
        // Tasks already generated stay in the history; they just lose the link to the chore.
        taskRepository.findByChoreId(choreId).forEach(task -> task.setChore(null));
        choreRepository.delete(chore);
    }

    /**
     * Creates this week's task for every active chore scheduled for {@code today}'s weekday that
     * hasn't been generated yet. Each chore is its own transaction, so one failure is logged and
     * the rest still run. Returns how many tasks were created.
     */
    public int generateDueTasks(LocalDate today) {
        List<UUID> dueToday = choreRepository.findByActiveTrue().stream()
                .filter(chore -> chore.getDayOfWeek() == today.getDayOfWeek())
                .map(RecurringChore::getId)
                .toList();

        int created = 0;
        for (UUID choreId : dueToday) {
            try {
                if (generator.generateIfDue(choreId, today)) {
                    created++;
                }
            } catch (RuntimeException e) {
                log.error("Could not generate the task for chore {}", choreId, e);
            }
        }
        return created;
    }

    private RecurringChore find(UUID roomId, UUID choreId) {
        return choreRepository.findByIdAndRoomId(choreId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chore not found"));
    }

    private ChoreResponse toResponse(RecurringChore c) {
        User nextUp = c.getAssignedTo();
        return new ChoreResponse(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getDayOfWeek(),
                nextUp == null ? null : nextUp.getId(),
                nextUp == null ? null : nextUp.getName(),
                c.isRotate(),
                c.isActive(),
                c.getLastGeneratedDate());
    }
}
