# ADR-0020: Table exports are an Excel file or a PDF, never a CSV

**Status:** Accepted · **Date:** 2026-10-16

**Context.** The exports of the drivers, vehicles, payments and recharges tables were semicolon CSV files. Opened in Excel they had no column widths, no number or date formats (amounts and dates were text), and the payments export had no button in the dashboard. The team shared two documents as models of what they expect: the fleet's recharge follow-up workbook (a title bar, dark header row, bordered banded rows, thousands separators, a total row) and the drivers list as a PDF (an amber title box over a bordered table).

**Decision.**
- Every table export is built by its feature's service as an `ExportTable` (title, subtitle, typed columns, rows, optional totals row) and written by `common/export`: `XlsxTableWriter` (Apache POI `poi-ooxml` 5.4.1) or `PdfTableWriter` (OpenPDF, already used for contracts and receipts). The endpoint takes `format=XLSX|PDF` (default `XLSX`). `ExportFile` carries the bytes and the `Content-Disposition`.
- The Excel file has the amber title bar, a subtitle with the row count and the export time, a dark header row, thin borders, banded rows, typed cells (`#,##0` amounts, dates, percents), a frozen header, a filter, and a bold totals row; it prints landscape on one page width with the header repeated. Text is written as text, so a cell like `=SUM(A1)` is never a formula. The PDF is A4, landscape when there are more than six columns, with the header repeated on each page and "Page x / y".
- The dashboard has one `ExportMenu` (Excel or PDF) next to the other page actions; the recharges screen exports the tab it is on. The payments export is on the Transactions tab (payments only, with the method, search and date filters).
- Every file starts with the country's brand (logo and name, from `CountryBrand`), the title, the dates covered ("du 01/10/2026 au 16/10/2026", "le …", or "toutes les dates") and the export time in the country's time zone (`ExportBranding`). Dates and times in the rows are converted to that time zone too.
- An export is what the page shows: it takes the same dates and search as the table it comes from (no separate period dialog). The recharges screen has two exports, because it has two sources: the manual recharges (`/charging/export`) and the network's sessions (`/charging/sessions/export`, button on the "Sessions du réseau" tab). The first version only exported the manual recharges, so a day with network sessions only gave an empty file.
- Authorization, the tenant filter, the 10 000 rows limit and the audit trail are unchanged. `CsvWriter` is removed.

**Consequences.** (+) Files open clean in Excel and print well; amounts can be summed. (-) A new dependency (POI, about 10 MB with its transitive jars). The whole file is built in memory, which is why the limit stays at 10 000 rows.

**Alternatives considered.** Keeping CSV and tuning it (no widths, no styles, no totals formatting: rejected). fastexcel (smaller, but no frozen panes or print setup).
