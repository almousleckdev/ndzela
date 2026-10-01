package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.config.properties.AppProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Private files (driver photos and documents): never served statically, the type comes from the bytes. */
@Slf4j
@Component
public class DocumentStorage {
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path directory;

    public DocumentStorage(AppProperties props) {
        this.directory = Path.of(props.storage().uploadDir()).toAbsolutePath().normalize().resolve("private");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload directory " + directory, e);
        }
    }

    public String save(MultipartFile file, boolean imageOnly) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_FILE", "No file received.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("FILE_TOO_LARGE", "The file must be 5 MB or smaller.");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = extensionOf(bytes);
            if (extension == null || (imageOnly && extension.equals("pdf"))) {
                throw new BusinessRuleViolationException("INVALID_FILE", imageOnly ? "Only PNG, JPEG or WebP images are accepted." : "Only PNG, JPEG, WebP or PDF files are accepted.");
            }
            String name = UUID.randomUUID() + "." + extension;
            Files.write(directory.resolve(name), bytes);
            return name;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }
    }

    public Resource load(String name) {
        Path target = resolve(name);
        if (target == null || !Files.isRegularFile(target)) {
            return null;
        }
        return new FileSystemResource(target);
    }

    public void delete(String name) {
        Path target = resolve(name);
        if (target == null) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete file {}", target, e);
        }
    }

    public static String contentType(String name) {
        return switch (name.substring(name.lastIndexOf('.') + 1)) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> "application/pdf";
        };
    }

    private Path resolve(String name) {
        if (name == null) {
            return null;
        }
        Path target = directory.resolve(name).normalize();
        return target.startsWith(directory) ? target : null;
    }

    private static String extensionOf(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        if (b.length > 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') return "pdf";
        return null;
    }
}
