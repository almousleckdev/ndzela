package com.ndzela.admin.vehicle.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.web.CsvWriter;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.domain.VehicleStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// The vehicles table as a CSV file, with the same filters; every export is audited.
@Service
@RequiredArgsConstructor
public class VehicleExportService {
    private static final Map<VehicleStatus, String> STATUS = Map.of(VehicleStatus.AVAILABLE, "Disponible", VehicleStatus.ASSIGNED, "Loué",
            VehicleStatus.IN_MAINTENANCE, "Maintenance", VehicleStatus.ACCIDENT, "Accidenté", VehicleStatus.SUSPENDED, "Suspendu",
            VehicleStatus.OUT_OF_SERVICE, "Hors service");

    private final VehicleService vehicleService;
    private final AuditService audit;

    @Transactional
    public byte[] csv(VehicleStatus status, String q, java.time.LocalDate from, java.time.LocalDate to) {
        List<VehicleResponse> rows = vehicleService.listForExport(status, q, from, to);
        List<List<String>> lines = rows.stream().map(v -> List.of(v.fleetNumber(), v.brand(), v.model(), String.valueOf(v.year()), v.plate(), v.color(),
                v.dailyRent().toPlainString(), v.purchasePrice().toPlainString(), STATUS.get(v.status()), v.statusReason() == null ? "" : v.statusReason())).toList();
        audit.record("VEHICLES_EXPORTED", "VEHICLE", null, "Export véhicules", TenantHolder.current().countryId(), lines.size() + " lignes");
        return CsvWriter.toBytes(List.of("Numéro", "Marque", "Modèle", "Année", "Immatriculation", "Couleur", "Loyer journalier (FCFA)", "Prix d'achat (FCFA)", "Statut", "Motif"), lines);
    }
}
