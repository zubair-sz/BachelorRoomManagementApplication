package com.brm.app.service;

import com.brm.app.dto.MediaPostResponse;
import com.brm.app.entity.MediaPost;
import com.brm.app.entity.Room;
import com.brm.app.entity.User;
import com.brm.app.exception.ForbiddenException;
import com.brm.app.exception.ResourceNotFoundException;
import com.brm.app.repository.MediaPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaService {

    private static final int MAX_CAPTION_LENGTH = 1000;
    private static final int MAX_PAGE_SIZE = 50;

    public record MediaFile(Resource resource, String contentType) {}

    private final MediaPostRepository postRepository;
    private final RoomAccessService access;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public List<MediaPostResponse> list(UUID roomId, User current, int page, int size) {
        access.requireMember(roomId, current);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "postedAt"));
        return postRepository.findByRoomId(roomId, pageable).getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    /** A post needs text, a photo/video, or both. */
    public MediaPostResponse create(UUID roomId, User current, String caption, MultipartFile file) {
        Room room = access.requireMember(roomId, current);

        String text = caption == null ? "" : caption.trim();
        boolean hasFile = file != null && !file.isEmpty();
        if (text.isEmpty() && !hasFile) {
            throw new IllegalArgumentException("Write something or attach a photo or video");
        }
        if (text.length() > MAX_CAPTION_LENGTH) {
            throw new IllegalArgumentException("Posts can be at most " + MAX_CAPTION_LENGTH + " characters");
        }

        FileStorageService.StoredFile stored = hasFile ? storage.store(file) : null;
        try {
            MediaPost post = MediaPost.builder()
                    .room(room)
                    .user(access.managed(current))
                    .caption(text.isEmpty() ? null : text)
                    .build();
            if (stored != null) {
                post.setStoredFilename(stored.storedName());
                post.setFileUrl("/rooms/" + roomId + "/media/files/" + stored.storedName());
                post.setContentType(stored.contentType());
                post.setFileType(stored.kind());
            }
            return toResponse(postRepository.save(post));
        } catch (RuntimeException e) {
            if (stored != null) {
                storage.delete(stored.storedName()); // don't leave an orphaned upload behind
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public MediaFile file(UUID roomId, User current, String filename) {
        access.requireMember(roomId, current);
        MediaPost post = postRepository.findByRoomIdAndStoredFilename(roomId, filename)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
        return new MediaFile(storage.load(post.getStoredFilename()), post.getContentType());
    }

    /** Authors can delete their own posts; the room admin can delete any. */
    public void delete(UUID roomId, UUID postId, User current) {
        Room room = access.requireMember(roomId, current);
        MediaPost post = postRepository.findByIdAndRoomId(postId, roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        boolean author = post.getUser().getId().equals(current.getId());
        if (!author && !access.isAdmin(room, current.getId())) {
            throw new ForbiddenException("You can only delete your own posts");
        }
        postRepository.delete(post);
        if (post.getStoredFilename() != null) {
            storage.delete(post.getStoredFilename());
        }
    }

    private MediaPostResponse toResponse(MediaPost post) {
        return new MediaPostResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getName(),
                post.getCaption(),
                post.getFileUrl(),
                post.getFileType(),
                post.getContentType(),
                post.getPostedAt());
    }
}
