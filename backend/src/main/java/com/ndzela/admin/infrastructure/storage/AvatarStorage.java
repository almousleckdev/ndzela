package com.ndzela.admin.infrastructure.storage;

import com.ndzela.admin.common.exception.BusinessRuleViolationException;
import com.ndzela.admin.common.exception.StorageUnavailableException;
import com.ndzela.admin.config.properties.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Profile pictures: public CDN images under <root>/<country or global>/avatars/. The database keeps the picture's URL.
@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarStorage {
    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final String GLOBAL = "global";

    private final CloudinaryClient cloud;
    private final AppProperties props;

    public String save(MultipartFile file, String countryCode) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("INVALID_IMAGE", "No file received.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("IMAGE_TOO_LARGE", "The image must be 2 MB or smaller.");
        }
        try {
            byte[] bytes = file.getBytes();
            detectExtension(bytes);
            String folder = countryCode == null ? GLOBAL : countryCode.toLowerCase(Locale.ROOT);
            return cloud.uploadPublicImage(bytes, props.storage().rootFolder() + "/" + folder + "/avatars/" + UUID.randomUUID());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded avatar", e);
        }
    }

    // Best effort, and only for pictures this application stored: any other URL is left alone.
    public void delete(String url) {
        String publicId = publicIdOf(url);
        if (publicId == null) {
            return;
        }
        try {
            cloud.deletePublicImage(publicId);
        } catch (StorageUnavailableException e) {
            log.warn("Could not delete the old avatar {}", publicId, e);
        }
    }

    String publicIdOf(String url) {
        if (url == null) {
            return null;
        }
        String prefix = "https://res.cloudinary.com/" + props.storage().cloudName() + "/image/upload/";
        if (!url.startsWith(prefix)) {
            return null;
        }
        Matcher match = Pattern.compile("(?:v\\d+/)?(" + Pattern.quote(props.storage().rootFolder()) + "/[A-Za-z0-9_/-]+)\\.[a-z]+").matcher(url.substring(prefix.length()));
        return match.matches() ? match.group(1) : null;
    }

    private static String detectExtension(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new BusinessRuleViolationException("INVALID_IMAGE", "Only PNG, JPEG or WebP images are accepted.");
    }
}
