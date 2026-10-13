package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.StorageUnavailableException;
import com.ndzela.admin.config.properties.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

// Private files (driver photos and documents): private Cloudinary assets under <root>/<country>/documents/.
// What the database keeps is "<country>/<uuid>.<ext>", which says where the file lives even when a super admin works across countries.
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStorage {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Pattern COUNTRY = Pattern.compile("[a-z]{2}");
    private static final Pattern NAME = Pattern.compile("([a-z]{2})/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:png|jpg|webp|pdf))");

    private final CloudinaryClient cloud;
    private final AppProperties props;

    public String save(MultipartFile file, boolean imageOnly, String countryCode) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_FILE", "No file received.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("FILE_TOO_LARGE", "The file must be 5 MB or smaller.");
        }
        String country = countryCode == null ? "" : countryCode.toLowerCase(Locale.ROOT);
        if (!COUNTRY.matcher(country).matches()) {
            throw new IllegalArgumentException("A file belongs to a country");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension = extensionOf(bytes);
            if (extension == null || (imageOnly && extension.equals("pdf"))) {
                throw new BusinessRuleViolationException("INVALID_FILE", imageOnly ? "Only PNG, JPEG or WebP images are accepted." : "Only PNG, JPEG, WebP or PDF files are accepted.");
            }
            String name = country + "/" + UUID.randomUUID() + "." + extension;
            cloud.uploadPrivateFile(bytes, publicId(name));
            return name;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded file", e);
        }
    }

    /** The file, or null when the name is not one of ours or the file is gone. */
    public Resource load(String name) {
        String publicId = name == null ? null : publicId(name);
        byte[] bytes = publicId == null ? null : cloud.downloadPrivateFile(publicId);
        return bytes == null ? null : new ByteArrayResource(bytes);
    }

    // Best effort: a file that could not be removed is only an orphan, and must not undo the change that made it unneeded.
    public void delete(String name) {
        String publicId = name == null ? null : publicId(name);
        if (publicId == null) {
            return;
        }
        try {
            cloud.deletePrivateFile(publicId);
        } catch (StorageUnavailableException e) {
            log.warn("Could not delete the stored file {}", name, e);
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

    // Anything that is not exactly what save() produces is refused, so a bad database value can never point elsewhere.
    private String publicId(String name) {
        var match = NAME.matcher(name);
        return match.matches() ? props.storage().rootFolder() + "/" + match.group(1) + "/documents/" + match.group(2) : null;
    }

    private static String extensionOf(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        if (b.length > 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') return "pdf";
        return null;
    }
}
