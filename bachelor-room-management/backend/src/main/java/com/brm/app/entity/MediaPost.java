package com.brm.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** A post on the shared room board: a text update, a photo/video, or both. */
@Entity
@Table(name = "media_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000)
    private String caption;

    /** API path the frontend can request (relative to /api), null for text-only posts. */
    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "stored_filename", length = 100)
    private String storedFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 20)
    private MediaKind fileType;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    @PrePersist
    void onCreate() {
        this.postedAt = Instant.now();
    }
}
