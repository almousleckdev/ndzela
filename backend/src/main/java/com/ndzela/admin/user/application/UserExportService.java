package com.ndzela.admin.user.application;

import com.ndzela.admin.audit.application.AuditService;
import com.ndzela.admin.common.export.ExportFile;
import com.ndzela.admin.common.export.ExportFormat;
import com.ndzela.admin.common.export.ExportTable;
import com.ndzela.admin.common.export.ExportTable.Column;
import com.ndzela.admin.country.application.ExportBranding;
import com.ndzela.admin.tenant.TenantHolder;
import com.ndzela.admin.user.api.dto.RoleRef;
import com.ndzela.admin.user.api.dto.UserSummary;
import com.ndzela.admin.user.domain.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// The users table as a spreadsheet or a PDF; every export is audited.
@Service
@RequiredArgsConstructor
public class UserExportService {
    private static final Map<UserStatus, String> STATUS = Map.of(UserStatus.MFA_SETUP, "Invitation en attente", UserStatus.ACTIVE, "Actif",
            UserStatus.LOCKED, "Verrouillé", UserStatus.DISABLED, "Désactivé");

    private final UserManagementService users;
    private final AuditService audit;
    private final ExportBranding branding;

    @Transactional
    public ExportFile export(ExportFormat format) {
        List<UserSummary> rows = users.list();
        List<List<Object>> lines = rows.stream().map(u -> Arrays.<Object>asList(u.email(), u.firstName() == null ? "" : u.firstName(), u.lastName() == null ? "" : u.lastName(),
                u.phoneNumber() == null ? "" : u.phoneNumber(), STATUS.get(u.status()), u.statusReason() == null ? "" : u.statusReason(),
                u.roles().stream().map(RoleRef::name).reduce((a, b) -> a + ", " + b).orElse(""), u.country() == null ? "Global" : u.country().name(),
                branding.day(u.createdAt()))).toList();
        audit.record("USERS_EXPORTED", "USER", null, "Export utilisateurs", TenantHolder.current().countryId(), lines.size() + " lignes");
        ExportTable table = new ExportTable("Liste des utilisateurs", branding.header(null, null), List.of(Column.text("E-mail", 28), Column.text("Prénom", 16), Column.text("Nom", 16),
                Column.text("Téléphone", 16), Column.text("Statut", 18), Column.text("Motif", 20), Column.text("Rôles", 22), Column.text("Pays", 12),
                Column.of("Créé le", ExportTable.Kind.DATE, 14)), lines, null);
        return ExportFile.of(table, format, "utilisateurs");
    }
}
