package com.ndzela.admin.charging.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.api.dto.RechargeResponse;
import com.ndzela.admin.charging.domain.ChargingEntry;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.charging.domain.ChargingEntryType;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.country.application.DayRanges;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The recharges as an Excel or PDF table, oldest first, with the total of those not cancelled.
@Service
@RequiredArgsConstructor
public class ChargingExportService {
    private static final PageRequest OLDEST_FIRST = PageRequest.of(0, 10_000, Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
    private static final List<ExportColumn> COLUMNS = List.of(ExportColumn.of("Date", ExportKind.DATE, 14), ExportColumn.text("Heure", 10),
            ExportColumn.text("Type", 12), ExportColumn.text("Véhicule", 12), ExportColumn.text("Chauffeur", 24), ExportColumn.text("Station", 18),
            ExportColumn.of("Montant (FCFA)", ExportKind.MONEY, 14), ExportColumn.text("Moyen", 14), ExportColumn.text("Référence", 18),
            ExportColumn.of("Batterie avant", ExportKind.PERCENT, 12), ExportColumn.of("Batterie après", ExportKind.PERCENT, 12),
            ExportColumn.of("kWh", ExportKind.DECIMAL, 8), ExportColumn.text("Motif", 22), ExportColumn.text("Statut", 10), ExportColumn.text("Saisi par", 18));

    private final ChargingEntryRepository entries;
    private final ChargingQueryService queries;
    private final DayRanges dayRanges;
    private final AuditService audit;
    private final ExportBranding branding;

    @Transactional
    public ExportFile export(String q, LocalDate from, LocalDate to, ExportFormat format) {
        List<ChargingEntry> rows = entries.findAll(dayRanges.<ChargingEntry>within("createdAt", from, to).and(queries.search(q)), OLDEST_FIRST).getContent();
        Set<UUID> reversed = queries.reversedAmong(rows);
        Map<UUID, RechargeResponse> detail = queries.responses(rows).stream().collect(Collectors.toMap(RechargeResponse::id, r -> r));
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        List<List<Object>> lines = new ArrayList<>();
        for (ChargingEntry e : rows) {
            RechargeResponse r = detail.get(e.getId());
            boolean live = e.getType() == ChargingEntryType.RECHARGE && !reversed.contains(e.getId());
            if (live) {
                total = total.add(e.getAmount());
                count++;
            }
            lines.add(Arrays.<Object>asList(branding.day(e.getCreatedAt()), branding.time(e.getCreatedAt()), e.getType() == ChargingEntryType.RECHARGE ? "Recharge" : "Annulation",
                    r.vehicle() == null ? "" : r.vehicle(), r.driverName() == null ? "" : r.driverName(), r.station() == null ? "" : r.station(), queries.whole(e.getAmount()),
                    e.getMethod() == null ? "" : e.getMethod().label(), e.getReference() == null ? "" : e.getReference(), e.getBatteryBefore(), e.getBatteryAfter(), e.getKwh(),
                    e.getReason() == null ? "" : e.getReason(), live ? "" : e.getType() == ChargingEntryType.RECHARGE ? "Annulée" : "", e.getRecordedBy()));
        }
        List<Object> totals = Arrays.<Object>asList("TOTAL (" + count + (count > 1 ? " recharges non annulées)" : " recharge non annulée)"), null, null, null, null, null, queries.whole(total), null, null, null, null, null, null, null, null);
        audit.record("CHARGING_EXPORTED", "CHARGING", null, "Export des recharges", TenantHolder.current().countryId(), "recharges, " + rows.size() + " lignes");
        ExportTable table = new ExportTable("Recharges", branding.header(from, to), COLUMNS, lines, totals);
        return ExportFile.of(table, format, "recharges" + suffix(from, to));
    }

    private static String suffix(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return "";
        }
        return from != null && from.equals(to) ? "-" + from : "-" + (from == null ? "" : from) + "_" + (to == null ? "" : to);
    }
}
