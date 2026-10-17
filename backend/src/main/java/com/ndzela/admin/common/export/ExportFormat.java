package com.ndzela.admin.common.export;

import org.springframework.http.MediaType;

// The formats the user can choose when exporting a table
public enum ExportFormat {
    XLSX("xlsx", MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    PDF("pdf", MediaType.APPLICATION_PDF);

    private final String extension;
    private final MediaType mediaType;

    ExportFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public MediaType mediaType() {
        return mediaType;
    }
}
