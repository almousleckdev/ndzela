package com.ndzela.admin.driver.application;

import com.ndzela.admin.common.export.ExportKind;
import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportColumn;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.driver.domain.Driver;
import com.ndzela.admin.driver.domain.DriverRepository;
import com.ndzela.admin.driver.domain.DriverStatus;
import com.ndzela.admin.driver.domain.VehicleAssignment;
import com.ndzela.admin.driver.domain.VehicleAssignmentRepository;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.vehicle.api.dto.VehicleResponse;
import com.ndzela.admin.vehicle.application.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// The drivers table as a spreadsheet or a PDF, with the same filters; every export is audited.
@Service
@RequiredArgsConstructor
public class DriverExportService {
    static final int MAX_ROWS = 10_000;
    private static final Map<DriverStatus, String> STATUS = Map.of(DriverStatus.ACTIVE, "Actif", DriverStatus.SUSPENDED, "Suspendu",
            DriverStatus.BLOCKED, "Bloqué", DriverStatus.INACTIVE, "Inactif");

    private final DriverRepository drivers;
    private final VehicleAssignmentRepository assignments;
    private final VehicleService vehicleService;
    private final DriverLeaveService leaveService;
    private final DriverFilters filters;
    private final AuditService audit;
    private final ExportBranding branding;

    @Transactional
    public ExportFile export(DriverStatus status, Boolean onLeave, String q, java.time.LocalDate from, java.time.LocalDate to, ExportFormat format) {
        List<Driver> rows = drivers.findAll(filters.spec(status, onLeave, null, q, from, to), PageRequest.of(0, MAX_ROWS, Sort.by("lastName", "firstName", "id"))).getContent();
        List<VehicleAssignment> open = assignments.findAllByDriverIdInAndEndedAtIsNull(rows.stream().map(Driver::getId).toList());
        Map<UUID, VehicleResponse> vehicles = vehicleService.responsesByIds(open.stream().map(VehicleAssignment::getVehicleId).toList());
        Map<UUID, UUID> vehicleOf = open.stream().collect(Collectors.toMap(VehicleAssignment::getDriverId, VehicleAssignment::getVehicleId, (a, b) -> a));
        Set<UUID> onLeaveIds = leaveService.onLeave(rows);
        List<List<Object>> lines = rows.stream().map(d -> {
            VehicleResponse v = vehicles.get(vehicleOf.get(d.getId()));
            return Arrays.<Object>asList(d.getLastName(), d.getFirstName(), d.getPhonePrimary(), d.getEmail(),
                    onLeaveIds.contains(d.getId()) ? "En arrêt" : STATUS.get(d.getStatus()), d.getStatusReason() == null ? "" : d.getStatusReason(),
                    v == null ? "" : v.fleetNumber(), v == null ? "" : v.plate(), d.getTrainingCenter() == null ? "" : d.getTrainingCenter().getName(), branding.day(d.getCreatedAt()));
        }).toList();
        audit.record("DRIVERS_EXPORTED", "DRIVER", null, "Export chauffeurs", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Liste des chauffeurs", branding.header(from, to), List.of(ExportColumn.text("Nom", 22), ExportColumn.text("Prénom", 20), ExportColumn.text("Téléphone", 16),
                ExportColumn.text("Email", 28), ExportColumn.text("Statut", 11), ExportColumn.text("Motif", 24), ExportColumn.text("Véhicule", 12), ExportColumn.text("Immatriculation", 15),
                ExportColumn.text("Centre de formation", 24), ExportColumn.of("Créé le", ExportKind.DATE, 14)), lines, null);
        return ExportFile.of(table, format, "chauffeurs");
    }
}
