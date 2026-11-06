package com.ndzela.admin.charging.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.charging.domain.ChargingEntryRepository;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.charging.api.dto.SessionSummary;
import com.ndzela.admin.charging.domain.ChargingPeriod;
import com.ndzela.admin.common.web.dto.PageResponse;
import com.ndzela.admin.country.application.CountryService;
import com.ndzela.admin.ocpp.api.SessionKind;
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
    private final CountryService countries;
    private final SessionHolders holders;
    private final SessionPayments walletPayments;

    @Transactional(readOnly = true)
    public PageResponse<SessionRow> list(String status, String q, LocalDate from, LocalDate to, SessionKind kind, int page, int size) {
        PageResponse<SessionRow> found = sessions.list(status, q, from, to, kind, holders.driverIdTags(), page, size);
        Map<UUID, UUID> paid = new HashMap<>();
        if (!found.items().isEmpty()) {
            entries.activeRechargesOfSessions(found.items().stream().map(SessionRow::id).toList()).forEach(row -> paid.put((UUID) row[0], (UUID) row[1]));
        }
        List<SessionRow> marked = walletPayments.mark(holders.mark(found.items().stream().map(row -> paid.containsKey(row.id()) ? row.withRecharge(paid.get(row.id())) : row).toList()));
        return new PageResponse<>(marked, found.page(), found.size(), found.total());
    }

    // Sessions started today, and the calculated amount of the day, the week (Monday to Sunday) and the month, in the country's time zone
    @Transactional(readOnly = true)
    public SessionSummary summary(SessionKind kind) {
        var today = TenantHolder.current().isCountry() ? LocalDate.now(countries.zoneOf(TenantHolder.current().countryId()))
                : countries.todayByCountry().values().stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        List<SessionRow> day = inPeriod(ChargingPeriod.DAY, today, kind);
        return new SessionSummary(day.size(), total(day), total(inPeriod(ChargingPeriod.WEEK, today, kind)), total(inPeriod(ChargingPeriod.MONTH, today, kind)));
    }

    private List<SessionRow> inPeriod(ChargingPeriod period, LocalDate reference, SessionKind kind) {
        return sessions.list(null, null, period.start(reference), period.end(reference).minusDays(1), kind, holders.driverIdTags(), 0, EXPORT_LIMIT).items();
    }

    private static BigDecimal total(List<SessionRow> rows) {
        return rows.stream().map(SessionRow::amount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    // The sessions of the period with their payment status, as an Excel file or a PDF, with the energy and amount totals
    @Transactional
    public ExportFile export(String status, String q, LocalDate from, LocalDate to, SessionKind kind, ExportFormat format) {
        List<SessionRow> rows = list(status, q, from, to, kind, 0, EXPORT_LIMIT).items();
        BigDecimal energy = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        List<List<Object>> lines = new ArrayList<>();
        for (SessionRow r : rows) {
            energy = r.energyKwh() == null ? energy : energy.add(r.energyKwh());
            amount = r.amount() == null ? amount : amount.add(r.amount());
            lines.add(Arrays.<Object>asList(branding.dateTime(r.startedAt()), branding.dateTime(r.endedAt()), r.chargePointId(), r.connectorId(), r.idTag(), r.holder() == null ? "Public" : "Chauffeur NDZela", r.holder() == null ? null : r.holder().driverName(), r.holder() == null ? null : r.holder().vehicle(), r.energyKwh(), duration(r),
                    r.amount(), r.payment() == null ? null : r.payment().total(), r.payment() == null ? null : stateLabel(r.payment().state()), r.socBefore(), r.socAfter(), SessionLabels.status(r.status()), SessionLabels.reason(r.stopReason()), payment(r), r.transactionId()));
        }
        List<Object> totals = Arrays.<Object>asList("TOTAL (" + rows.size() + (rows.size() > 1 ? " sessions)" : " session)"), null, null, null, null, null, null, null, energy, null, amount, null, null, null, null, null, null, null, null);
        audit.record("CHARGING_EXPORTED", "CHARGING", null, "Export des sessions du réseau", TenantHolder.current().countryId(), "sessions, " + rows.size() + " lignes");
        ExportTable table = new ExportTable("Sessions du réseau", branding.header(from, to), List.of(ExportColumn.text("Début", 16), ExportColumn.text("Fin", 16), ExportColumn.text("Borne", 16), ExportColumn.text("Connecteur", 11),
                ExportColumn.text("Identifiant", 14), ExportColumn.text("Type", 16), ExportColumn.text("Chauffeur", 20), ExportColumn.text("Véhicule", 18), ExportColumn.of("Énergie (kWh)", ExportKind.DECIMAL, 11), ExportColumn.text("Durée", 9), ExportColumn.of("Montant calculé (FCFA)", ExportKind.MONEY, 14), ExportColumn.of("Total payé (FCFA)", ExportKind.MONEY, 14), ExportColumn.text("État du paiement Zella", 18),
                ExportColumn.of("Batterie avant", ExportKind.PERCENT, 11), ExportColumn.of("Batterie après", ExportKind.PERCENT, 11), ExportColumn.text("Statut", 13), ExportColumn.text("Motif d'arrêt", 18),
                ExportColumn.text("Paiement", 14), ExportColumn.text("Référence", 12)), lines, totals);
        return ExportFile.of(table, format, "sessions-reseau" + (from == null ? "" : "-" + from) + (to == null || to.equals(from) ? "" : "_" + to));
    }

    private static String stateLabel(String state) {
        return state == null ? "" : switch (state) {
            case "CHARGED" -> "Payé";
            case "PENDING" -> "En attente";
            case "NOT_CHARGED" -> "Non débité";
            case "UNDER_REVIEW" -> "En vérification";
            case "REFUNDED" -> "Remboursé";
            default -> state;
        };
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
