package com.ndzela.admin.charging.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.ocpp.api.SessionRow;
import com.ndzela.admin.ocpp.application.OcppSessionQueryService;
import com.ndzela.admin.tenant.TenantHolder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// The network's sessions as staff see them, each marked with the recharge that recorded its payment, if any
@Service
@RequiredArgsConstructor
public class ChargingSessionService {
    private static final int EXPORT_LIMIT = 10_000;

    private final OcppSessionQueryService sessions;
    private final ChargingEntryRepository entries;
    private final ExportBranding branding;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<SessionRow> list(String status, String q, LocalDate from, LocalDate to, int page, int size) {
        PageResponse<SessionRow> found = sessions.list(status, q, from, to, page, size);
        Map<UUID, UUID> paid = new HashMap<>();
        if (!found.items().isEmpty()) {
            entries.activeRechargesOfSessions(found.items().stream().map(SessionRow::id).toList()).forEach(row -> paid.put((UUID) row[0], (UUID) row[1]));
        }
        List<SessionRow> marked = found.items().stream().map(row -> paid.containsKey(row.id()) ? row.withRecharge(paid.get(row.id())) : row).toList();
        return new PageResponse<>(marked, found.page(), found.size(), found.total());
    }

    // The sessions of the period with their payment status, as an Excel file or a PDF, with the energy and amount totals
    @Transactional
    public ExportFile export(String status, String q, LocalDate from, LocalDate to, ExportFormat format) {
        List<SessionRow> rows = list(status, q, from, to, 0, EXPORT_LIMIT).items();
        BigDecimal energy = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        List<List<Object>> lines = new ArrayList<>();
        for (SessionRow r : rows) {
            energy = r.energyKwh() == null ? energy : energy.add(r.energyKwh());
            amount = r.amount() == null ? amount : amount.add(r.amount());
            lines.add(Arrays.<Object>asList(branding.dateTime(r.startedAt()), branding.dateTime(r.endedAt()), r.chargePointId(), r.connectorId(), r.idTag(), r.energyKwh(), duration(r),
                    r.amount(), r.socBefore(), r.socAfter(), SessionLabels.status(r.status()), SessionLabels.reason(r.stopReason()), payment(r), r.transactionId()));
        }
        List<Object> totals = Arrays.<Object>asList("TOTAL (" + rows.size() + (rows.size() > 1 ? " sessions)" : " session)"), null, null, null, null, energy, null, amount, null, null, null, null, null, null);
        audit.record("CHARGING_EXPORTED", "CHARGING", null, "Export des sessions du réseau", TenantHolder.current().countryId(), "sessions, " + rows.size() + " lignes");
        ExportTable table = new ExportTable("Sessions du réseau", branding.header(from, to), List.of(ExportColumn.text("Début", 16), ExportColumn.text("Fin", 16), ExportColumn.text("Borne", 16), ExportColumn.text("Connecteur", 11),
                ExportColumn.text("Identifiant", 14), ExportColumn.of("Énergie (kWh)", ExportKind.DECIMAL, 11), ExportColumn.text("Durée", 9), ExportColumn.of("Montant calculé (FCFA)", ExportKind.MONEY, 14),
                ExportColumn.of("Batterie avant", ExportKind.PERCENT, 11), ExportColumn.of("Batterie après", ExportKind.PERCENT, 11), ExportColumn.text("Statut", 13), ExportColumn.text("Motif d'arrêt", 18),
                ExportColumn.text("Paiement", 14), ExportColumn.text("Référence", 12)), lines, totals);
        return ExportFile.of(table, format, "sessions-reseau" + (from == null ? "" : "-" + from) + (to == null || to.equals(from) ? "" : "_" + to));
    }

    private static String duration(SessionRow r) {
        if (r.durationSeconds() == null) {
            return "";
        }
        long minutes = Math.round(r.durationSeconds() / 60.0);
        return minutes < 60 ? minutes + " min" : (minutes / 60) + " h " + String.format("%02d", minutes % 60);
    }

    private static String payment(SessionRow r) {
        return r.rechargeId() != null ? "Payée" : r.startedAt() != null && r.endedAt() == null ? "En cours" : "Non enregistrée";
    }
}
