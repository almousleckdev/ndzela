package com.ndzela.admin.common.export;

// The weight is the column's share of the width: about the number of characters it needs
public record ExportColumn(String header, ExportKind kind, int weight) {
    public static ExportColumn text(String header, int weight) {
        return new ExportColumn(header, ExportKind.TEXT, weight);
    }

    public static ExportColumn of(String header, ExportKind kind, int weight) {
        return new ExportColumn(header, kind, weight);
    }
}
