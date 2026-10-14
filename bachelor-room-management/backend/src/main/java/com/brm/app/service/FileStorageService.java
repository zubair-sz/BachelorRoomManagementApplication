package com.brm.app.service;

import com.brm.app.entity.MediaKind;
import com.brm.app.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stores uploads on the local disk. To move to S3/Firebase later, keep this class's public
 * methods and swap the implementation; nothing else touches the filesystem.
 *
 * Only a fixed list of photo/video types is accepted (SVG and HTML are deliberately excluded
 * because they can carry scripts), files are renamed to random UUIDs, and the content type that
 * is served back comes from that list, never from what the client claimed.
 */
@Slf4j
@Service
public class FileStorageService {

    public record StoredFile(String storedName, String contentType, MediaKind kind) {}

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "mp4", "video/mp4",
            "webm", "video/webm",
            "mov", "video/quicktime");

    private static final Pattern STORED_NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{2,5}$");

    private final Path root;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload directory " + root, e);
        }
    }

    public StoredFile store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String extension = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);

        String contentType = ALLOWED_TYPES.get(extension);
        if (contentType == null) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: JPG, PNG, GIF, WEBP, MP4, WEBM, MOV");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the uploaded file", e);
        }

        MediaKind kind = contentType.startsWith("video/") ? MediaKind.VIDEO : MediaKind.IMAGE;
        return new StoredFile(storedName, contentType, kind);
    }

    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (path == null || !Files.isReadable(path)) {
            throw new ResourceNotFoundException("File not found");
        }
        return new FileSystemResource(path);
    }

    /** Best effort: a leftover file is harmless, a failed delete must not break the request. */
    public void delete(String storedName) {
        try {
            Path path = resolve(storedName);
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("Could not delete uploaded file {}", storedName, e);
        }
    }

    /** Returns null for anything that isn't a name this service generated (blocks path traversal). */
    private Path resolve(String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            return null;
        }
        Path path = root.resolve(storedName).normalize();
        return path.startsWith(root) ? path : null;
    }
}
