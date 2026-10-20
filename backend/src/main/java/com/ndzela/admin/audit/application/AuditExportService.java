package com.ndzela.admin.audit.application;

import com.ndzela.admin.audit.domain.AuditLog;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// The audit log as a spreadsheet or a PDF, with the same filters as the screen; every export is itself audited.
@Service
@RequiredArgsConstructor
public class AuditExportService {
    private final AuditQueryService queries;
    private final AuditService audit;
    private final ExportBranding branding;

    @Transactional
    public ExportFile export(String action, String actor, String targetType, LocalDate from, LocalDate to, ExportFormat format) {
        List<AuditLog> rows = queries.forExport(action, actor, targetType, from, to);
        List<List<Object>> lines = rows.stream().map(a -> Arrays.<Object>asList(branding.dateTime(a.getCreatedAt()), a.getActor(), a.getAction(),
                a.getTarget() == null ? "" : a.getTarget(), a.getResult(), a.getReason() == null ? (a.getDetails() == null ? "" : a.getDetails()) : a.getReason())).toList();
        audit.record("AUDIT_EXPORTED", "AUDIT_LOG", null, "Export journal d'audit", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Journal d'audit", branding.header(from, to), List.of(ExportColumn.text("Date", 18), ExportColumn.text("Acteur", 24), ExportColumn.text("Action", 22),
                ExportColumn.text("Cible", 24), ExportColumn.text("Résultat", 10), ExportColumn.text("Détail", 30)), lines, null);
        return ExportFile.of(table, format, "audit");
    }
}
