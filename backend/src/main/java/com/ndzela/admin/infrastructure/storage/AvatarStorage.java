package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.properties.AppProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Stores profile pictures on disk.
@Slf4j
@Component
public class AvatarStorage {
    public static final String URL_PREFIX = "/media/avatars/";
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private final Path directory;

    public AvatarStorage(AppProperties props) {
        this.directory = Path.of(props.storage().uploadDir()).toAbsolutePath().normalize().resolve("avatars");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + directory, e);
        }
    }

    public Path rootDirectory() {
        return directory.getParent();
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_IMAGE", "No file received.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("IMAGE_TOO_LARGE", "The image must be 2 MB or smaller.");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = detectExtension(bytes);
            String name = UUID.randomUUID() + "." + extension;
            Files.write(directory.resolve(name), bytes);
            return URL_PREFIX + name;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store avatar", e);
        }
    }

    public void delete(String urlPath) {
        if (urlPath == null || !urlPath.startsWith(URL_PREFIX)) {
            return;
        }
        Path target = directory.resolve(urlPath.substring(URL_PREFIX.length())).normalize();
        if (!target.startsWith(directory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete old avatar {}", target, e);
        }
    }

    private static String detectExtension(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new BusinessRuleViolationException("INVALID_IMAGE", "Only PNG, JPEG or WebP images are accepted.");
    }
}
