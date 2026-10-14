package com.brm.app.repository;

import com.brm.app.entity.MediaPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaPostRepository extends JpaRepository<MediaPost, UUID> {
    Page<MediaPost> findByRoomId(UUID roomId, Pageable pageable);
    Optional<MediaPost> findByIdAndRoomId(UUID id, UUID roomId);
    Optional<MediaPost> findByRoomIdAndStoredFilename(UUID roomId, String storedFilename);
}
