package com.ndzela.admin.config.properties;

import java.time.Duration;
import java.util.regex.Pattern;

// Where files live (Cloudinary). The root folder separates environments inside one Cloudinary account: ndzela-dev, ndzela-prod.
public record StorageProperties(String cloudName, String apiKey, String apiSecret, String rootFolder, Duration requestTimeout) {
    private static final Pattern FOLDER = Pattern.compile("[a-z0-9][a-z0-9_-]{0,48}");

    public StorageProperties {
        if (rootFolder == null || !FOLDER.matcher(rootFolder).matches()) {
            throw new IllegalStateException("CLOUDINARY_ROOT_FOLDER must be lower-case letters, digits, '-' or '_' (e.g. ndzela-dev)");
        }
    }

    @Override
    public String toString() {
        return "StorageProperties[secrets hidden]";
    }
}
