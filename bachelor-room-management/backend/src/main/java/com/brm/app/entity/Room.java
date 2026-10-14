package com.brm.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(unique = true, length = 20)
    private String inviteCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.inviteCode == null) {
            this.inviteCode = generateInviteCode();
        }
    }
}
