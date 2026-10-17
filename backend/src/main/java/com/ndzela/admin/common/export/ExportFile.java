package com.ndzela.admin.common.export;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

// The bytes of an export and how the browser should save them
public record ExportFile(byte[] content, String baseName, ExportFormat format) {

    public static ExportFile of(ExportTable table, ExportFormat format, String baseName) {
        byte[] bytes = format == ExportFormat.PDF ? PdfTableWriter.write(table) : XlsxTableWriter.write(table);
        return new ExportFile(bytes, baseName, format);
    }

    public ResponseEntity<byte[]> response() {
        return ResponseEntity.ok().contentType(format.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(baseName + "." + format.extension(), StandardCharsets.UTF_8).build().toString())
                .body(content);
    }
}
