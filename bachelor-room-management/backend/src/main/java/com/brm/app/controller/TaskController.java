package com.brm.app.controller;

import com.brm.app.dto.TaskRequest;
import com.brm.app.dto.TaskResponse;
import com.brm.app.dto.TaskStatusRequest;
import com.brm.app.entity.User;
import com.brm.app.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> list(@PathVariable UUID roomId,
                                   @RequestParam(defaultValue = "false") boolean mine,
                                   @AuthenticationPrincipal User user) {
        return taskService.list(roomId, user, mine);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@PathVariable UUID roomId,
                                               @Valid @RequestBody TaskRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(roomId, user, request));
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable UUID roomId, @PathVariable UUID taskId,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal User user) {
        return taskService.update(roomId, taskId, user, request);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResponse setStatus(@PathVariable UUID roomId, @PathVariable UUID taskId,
                                  @Valid @RequestBody TaskStatusRequest request,
                                  @AuthenticationPrincipal User user) {
        return taskService.setStatus(roomId, taskId, user, request.status());
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roomId, @PathVariable UUID taskId,
                                       @AuthenticationPrincipal User user) {
        taskService.delete(roomId, taskId, user);
        return ResponseEntity.noContent().build();
    }
}
