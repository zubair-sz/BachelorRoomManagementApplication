package com.brm.app.service;

import com.brm.app.entity.RecurringChore;
import com.brm.app.entity.Task;
import com.brm.app.entity.TaskStatus;
import com.brm.app.entity.User;
import com.brm.app.repository.RecurringChoreRepository;
import com.brm.app.repository.TaskRepository;
import com.brm.app.util.ChoreRotation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns one recurring chore into this week's task. Lives in its own bean so each chore runs in
 * its own transaction: one broken chore can't stop the others from being generated.
 */
@Component
@RequiredArgsConstructor
public class ChoreGenerator {

    private final RecurringChoreRepository choreRepository;
    private final TaskRepository taskRepository;
    private final RoomAccessService access;

    /** Own transaction per chore. Returns false when nothing was due (wrong day, paused, already done). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean generateIfDue(UUID choreId, LocalDate today) {
        RecurringChore chore = choreRepository.findById(choreId).orElse(null);
        if (chore == null
                || !chore.isActive()
                || chore.getDayOfWeek() != today.getDayOfWeek()
                || today.equals(chore.getLastGeneratedDate())) {
            return false;
        }
        generateFor(chore, today);
        return true;
    }

    /** Creates the task and, for rotating chores, passes the chore on to the next roommate. */
    @Transactional
    public void generateFor(RecurringChore chore, LocalDate today) {
        Map<UUID, User> members = access.memberUsers(chore.getRoom().getId());
        List<UUID> order = new ArrayList<>(members.keySet());

        UUID nextUpId = chore.getAssignedTo() == null ? null : chore.getAssignedTo().getId();
        UUID assigneeId = ChoreRotation.resolveAssignee(order, nextUpId, chore.isRotate());

        taskRepository.save(Task.builder()
                .room(chore.getRoom())
                .assignedTo(assigneeId == null ? null : members.get(assigneeId))
                .createdBy(chore.getCreatedBy())
                .title(chore.getTitle())
                .description(chore.getDescription())
                .status(TaskStatus.PENDING)
                .dueDate(today)
                .recurring(true)
                .chore(chore)
                .build());
        chore.setLastGeneratedDate(today);

        if (chore.isRotate() && assigneeId != null) {
            chore.setAssignedTo(members.get(ChoreRotation.next(order, assigneeId)));
        }
    }
}
