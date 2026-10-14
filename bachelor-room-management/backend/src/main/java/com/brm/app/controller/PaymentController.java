package com.brm.app.controller;

import com.brm.app.dto.PaymentRequest;
import com.brm.app.dto.PaymentResponse;
import com.brm.app.entity.User;
import com.brm.app.service.PaymentService;
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
@RequestMapping("/api/rooms/{roomId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentResponse> list(@PathVariable UUID roomId, @AuthenticationPrincipal User user) {
        return paymentService.list(roomId, user);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> log(@PathVariable UUID roomId,
                                               @Valid @RequestBody PaymentRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.log(roomId, user, request));
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentResponse confirm(@PathVariable UUID roomId, @PathVariable UUID paymentId,
                                   @AuthenticationPrincipal User user) {
        return paymentService.confirm(roomId, paymentId, user);
    }

    @PostMapping("/{paymentId}/reject")
    public PaymentResponse reject(@PathVariable UUID roomId, @PathVariable UUID paymentId,
                                  @AuthenticationPrincipal User user) {
        return paymentService.reject(roomId, paymentId, user);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID roomId, @PathVariable UUID paymentId,
                                       @AuthenticationPrincipal User user) {
        paymentService.cancel(roomId, paymentId, user);
        return ResponseEntity.noContent().build();
    }
}
