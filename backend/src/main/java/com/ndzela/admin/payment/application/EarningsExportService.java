package com.ndzela.admin.payment.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.payment.api.dto.EarningsDriverRow;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// The earnings page as an Excel or PDF table: one line per paying driver, then the total.
@Service
@RequiredArgsConstructor
public class EarningsExportService {
    private static final int EXPORT_LIMIT = 10_000;
    private static final List<ExportColumn> COLUMNS = List.of(ExportColumn.text("Chauffeur", 26), ExportColumn.text("Statut", 12), ExportColumn.text("Contrat", 16),
            ExportColumn.of("Total payé (FCFA)", ExportKind.MONEY, 18), ExportColumn.text("Dernier paiement", 18));

    private final EarningsService earnings;
    private final ExportBranding branding;
    private final AuditService audit;

    @Transactional
    public ExportFile export(LocalDate from, LocalDate to, ExportFormat format) {
        List<EarningsDriverRow> rows = earnings.drivers(null, from, to, "name,asc", 0, EXPORT_LIMIT).items();
        List<List<Object>> lines = rows.stream().map(r -> Arrays.<Object>asList(r.name(), r.former() ? "Ancien" : "Actuel", r.contractNumber() == null ? "" : r.contractNumber(),
                r.totalPaid(), r.lastPayment() == null ? "" : branding.dateTime(r.lastPayment()))).toList();
        List<Object> totals = Arrays.<Object>asList("Total encaissé", "", "", earnings.summary(from, to).collected(), "");
        audit.record("EARNINGS_EXPORTED", "PAYMENT", null, "Export du total encaissé", TenantHolder.current().countryId(), lines.size() + " lignes");
        return ExportFile.of(new ExportTable("Total encaissé", branding.header(from, to), COLUMNS, lines, totals), format, "total-encaisse");
    }
}
