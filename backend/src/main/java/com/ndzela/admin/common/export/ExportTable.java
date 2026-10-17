package com.ndzela.admin.common.export;

import java.util.List;

// A table ready to be written as a spreadsheet or a PDF. Cells are String, Number (numbers stay numbers in Excel), LocalDate or null.
public record ExportTable(String title, Header header, List<Column> columns, List<List<Object>> rows, List<Object> totals) {

    // What sits on top of every export: the brand's name and logo (null: none), the dates the table covers, and when it was exported
    public record Header(String brand, byte[] logo, String period, String exportedAt) {}

    public enum Kind { TEXT, DATE, INTEGER, DECIMAL, MONEY, PERCENT }

    // The weight is the column's share of the width: about the number of characters it needs
    public record Column(String header, Kind kind, int weight) {
        public static Column text(String header, int weight) {
            return new Column(header, Kind.TEXT, weight);
        }

        public static Column of(String header, Kind kind, int weight) {
            return new Column(header, kind, weight);
        }
    }

    /** "Période : ... · Exporté le ... · 12 lignes" */
    public String info() {
        int count = rows.size();
        return "Période : " + header.period() + " · Exporté le " + header.exportedAt() + " · " + count + (count > 1 ? " lignes" : " ligne");
    }

    /** How many columns the label of the totals row covers: from the first one up to the first filled cell after it. */
    public int totalsLabelSpan() {
        if (totals == null || !(totals.get(0) instanceof String)) {
            return 1;
        }
        int span = 1;
        while (span < totals.size() && totals.get(span) == null) {
            span++;
        }
        return span;
    }
}
