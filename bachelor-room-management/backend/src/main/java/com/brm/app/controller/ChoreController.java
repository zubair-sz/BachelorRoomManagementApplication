package com.brm.app.controller;

import com.brm.app.dto.ChoreActiveRequest;
import com.brm.app.dto.ChoreRequest;
import com.brm.app.dto.ChoreResponse;
import com.brm.app.entity.User;
import com.brm.app.service.ChoreService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/chores")
@RequiredArgsConstructor
public class ChoreController {

    private final ChoreService choreService;

    @GetMapping
    public List<ChoreResponse> list(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return choreService.list(roomId, user);
    }

    @PostMapping
    public ResponseEntity<ChoreResponse> create(@PathVariable UUID roomId,
                                                @Valid @RequestBody ChoreRequest request,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(choreService.create(roomId, user, request));
    }

    @PatchMapping("/{choreId}")
    public ChoreResponse setActive(@PathVariable UUID roomId, @PathVariable UUID choreId,
                                   @Valid @RequestBody ChoreActiveRequest request,
                                   @AuthenticationPrincipal User user) {
        return choreService.setActive(roomId, choreId, user, request.active());
    }

    @DeleteMapping("/{choreId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roomId, @PathVariable UUID choreId,
                                       @AuthenticationPrincipal User user) {
        choreService.delete(roomId, choreId, user);
        return ResponseEntity.noContent().build();
    }
}
