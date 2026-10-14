package com.brm.app.controller;

import com.brm.app.dto.MediaPostResponse;
import com.brm.app.entity.User;
import com.brm.app.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms/{roomId}/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping
    public List<MediaPostResponse> list(@PathVariable UUID roomId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @AuthenticationPrincipal User user) {
        return mediaService.list(roomId, user, page, size);
    }

    /** Multipart form: optional "caption" text and optional "file" (photo or video), at least one of them. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaPostResponse> create(@PathVariable UUID roomId,
                                                    @RequestParam(value = "caption", required = false) String caption,
                                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.create(roomId, user, caption, file));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> file(@PathVariable UUID roomId, @PathVariable String filename,
                                         @AuthenticationPrincipal User user) {
        MediaService.MediaFile media = mediaService.file(roomId, user, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(media.resource());
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roomId, @PathVariable UUID postId,
                                       @AuthenticationPrincipal User user) {
        mediaService.delete(roomId, postId, user);
        return ResponseEntity.noContent().build();
    }
}
