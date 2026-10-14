package com.brm.app.repository;

import com.brm.app.entity.Payment;
import com.brm.app.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    List<Payment> findByRoomIdAndStatus(UUID roomId, PaymentStatus status);
    Optional<Payment> findByIdAndRoomId(UUID id, UUID roomId);
}
