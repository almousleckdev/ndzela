package com.ndzela.admin.payment.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.driver.domain.ArrearsLevel;
import com.ndzela.admin.payment.api.dto.AccountRow;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// The drivers' accounts as an Excel or PDF table.
@Service
@RequiredArgsConstructor
public class AccountExportService {
    private static final int EXPORT_LIMIT = 10_000;
    private static final Map<ArrearsLevel, String> LEVEL_LABEL = Map.of(ArrearsLevel.NONE, "À jour", ArrearsLevel.LATE, "En retard",
            ArrearsLevel.YELLOW, "Rappel", ArrearsLevel.DARK_YELLOW, "Urgent", ArrearsLevel.RED, "Suspendu");
    private static final List<ExportColumn> COLUMNS = List.of(ExportColumn.text("Chauffeur", 22), ExportColumn.text("Téléphone", 16), ExportColumn.text("Véhicule", 14),
            ExportColumn.of("Loyer (FCFA)", ExportKind.MONEY, 12), ExportColumn.of("Jours payés", ExportKind.INTEGER, 10),
            ExportColumn.of("Jours impayés", ExportKind.INTEGER, 10), ExportColumn.of("Jours restants", ExportKind.INTEGER, 10),
            ExportColumn.of("Montant dû (FCFA)", ExportKind.MONEY, 14), ExportColumn.of("À régler avant midi (FCFA)", ExportKind.MONEY, 16),
            ExportColumn.text("En retard depuis", 18), ExportColumn.text("Dernier paiement", 18), ExportColumn.text("Niveau", 10));

    private final AccountQueryService accounts;
    private final ExportBranding branding;
    private final AuditService audit;

    @Transactional
    public ExportFile export(String filter, String q, LocalDate lateFrom, LocalDate lateTo, LocalDate paidFrom, LocalDate paidTo, ExportFormat format) {
        List<AccountRow> rows = accounts.accounts(filter, q, lateFrom, lateTo, paidFrom, paidTo, null, 0, EXPORT_LIMIT).items();
        List<List<Object>> lines = rows.stream().map(this::line).toList();
        audit.record("ACCOUNTS_EXPORTED", "PAYMENT", null, "Export des comptes chauffeurs", TenantHolder.current().countryId(), lines.size() + " lignes");
        var header = branding.header(paidFrom != null ? paidFrom : lateFrom, paidTo != null ? paidTo : lateTo);
        return ExportFile.of(new ExportTable("Comptes des chauffeurs", header, COLUMNS, lines, null), format, "comptes-chauffeurs");
    }

    private List<Object> line(AccountRow r) {
        Function<OffsetDateTime, String> when = at -> at == null ? "" : branding.dateTime(at);
        return Arrays.<Object>asList(r.driverName(), r.phone(), r.vehicle() == null ? "" : r.vehicle(), r.dailyRent() == null ? null : whole(r.dailyRent()),
                r.paidDays(), r.unpaidDays(), r.daysLeft(), whole(r.overdueAmount()), whole(r.awaitingAmount()), when.apply(r.overdueSince()),
                when.apply(r.lastPaymentAt()), LEVEL_LABEL.get(r.level()));
    }

    private static BigDecimal whole(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
