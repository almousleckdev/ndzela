package com.ndzela.admin.vehicle.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.common.text.ColorNames;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// The vehicles table as a spreadsheet or a PDF, with the same filters; every export is audited.
@Service
@RequiredArgsConstructor
public class VehicleExportService {
    private static final Map<VehicleStatus, String> STATUS = Map.of(VehicleStatus.AVAILABLE, "Disponible", VehicleStatus.ASSIGNED, "Loué",
            VehicleStatus.IN_MAINTENANCE, "Maintenance", VehicleStatus.ACCIDENT, "Accidenté", VehicleStatus.SUSPENDED, "Suspendu",
            VehicleStatus.OUT_OF_SERVICE, "Hors service");

    private final VehicleQueryService vehicleQueries;
    private final AuditService audit;
    private final ExportBranding branding;

    @Transactional
    public ExportFile export(VehicleStatus status, String q, java.time.LocalDate from, java.time.LocalDate to, ExportFormat format) {
        List<VehicleResponse> rows = vehicleQueries.listForExport(status, q, from, to);
        List<List<Object>> lines = rows.stream().map(v -> Arrays.<Object>asList(v.fleetNumber(), v.brand(), v.model(), String.valueOf(v.year()), v.plate(), ColorNames.of(v.color()),
                v.dailyRent(), v.purchasePrice(), STATUS.get(v.status()), v.statusReason() == null ? "" : v.statusReason())).toList();
        audit.record("VEHICLES_EXPORTED", "VEHICLE", null, "Export véhicules", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Liste des véhicules", branding.header(from, to), List.of(ExportColumn.text("Numéro", 10), ExportColumn.text("Marque", 14), ExportColumn.text("Modèle", 16),
                ExportColumn.text("Année", 8), ExportColumn.text("Immatriculation", 15), ExportColumn.text("Couleur", 12), ExportColumn.of("Loyer journalier (FCFA)", ExportKind.MONEY, 14),
                ExportColumn.of("Prix d'achat (FCFA)", ExportKind.MONEY, 16), ExportColumn.text("Statut", 12), ExportColumn.text("Motif", 24)), lines, null);
        return ExportFile.of(table, format, "vehicules");
    }
}
