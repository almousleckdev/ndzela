package com.ndzela.admin.common.web;

import java.nio.charset.StandardCharsets;
import java.util.List;

// Excel-friendly CSV (UTF-8 with BOM, semicolon separated).
public final class CsvWriter {
    private CsvWriter() {}

    public static byte[] toBytes(List<String> headers, List<List<String>> rows) {
        StringBuilder out = new StringBuilder("﻿");
        out.append(line(headers));
        rows.forEach(row -> out.append(line(row)));
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String line(List<String> cells) {
        return String.join(";", cells.stream().map(CsvWriter::cell).toList()) + "\r\n";
    }

    private static String cell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
