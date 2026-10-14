package com.brm.app.controller;

import com.brm.app.dto.BalanceResponse;
import com.brm.app.dto.ExpenseRequest;
import com.brm.app.dto.ExpenseResponse;
import com.brm.app.entity.User;
import com.brm.app.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping("/expenses")
    public List<ExpenseResponse> list(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return expenseService.list(roomId, user);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseResponse> create(@PathVariable UUID roomId,
                                                  @Valid @RequestBody ExpenseRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(roomId, user, request));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roomId, @PathVariable UUID expenseId,
                                       @AuthenticationPrincipal User user) {
        expenseService.delete(roomId, user, expenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balances")
    public List<BalanceResponse> balances(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return expenseService.balances(roomId, user);
    }
}
