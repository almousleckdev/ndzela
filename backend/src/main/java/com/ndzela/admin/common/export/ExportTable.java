package com.ndzela.admin.common.export;

import java.util.List;

// A table ready to be written as a spreadsheet or a PDF. Cells are String, Number (numbers stay numbers in Excel), LocalDate or null.
public record ExportTable(String title, ExportHeader header, List<ExportColumn> columns, List<List<Object>> rows, List<Object> totals) {

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
