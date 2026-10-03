"use client";

import { useState } from "react";
import { RequirePermission } from "@/features/access/components/require-permission";
import { RoleDetail } from "@/features/roles/components/role-detail";
import { RoleFormDialog } from "@/features/roles/components/role-form-dialog";
import { RoleRail } from "@/features/roles/components/role-rail";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { rolesService } from "@/features/roles/services/roles.service";
import { usersService } from "@/features/users/services/users.service";
import type { RoleSummary } from "@/features/roles/types";

function RolesContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const canListUsers = can(PERMISSIONS.USERS_READ);
  const roles = useFetch(rolesService.listVisible);
  const catalogue = useFetch(rolesService.permissions);
  const users = useFetch(() => (canListUsers ? usersService.list() : Promise.resolve([])));

  const [selectedId, setSelectedId] = useState<string>();
  const [editing, setEditing] = useState<RoleSummary | "new" | null>(null);
  const [deleting, setDeleting] = useState<RoleSummary | null>(null);

  // Most powerful first, so the hierarchy reads top to bottom.
  const list = [...(roles.data ?? [])].sort((a, b) => b.permissions.length - a.permissions.length || a.name.localeCompare(b.name));
  const selected = list.find((r) => r.id === selectedId) ?? list[0];
  const permissions = catalogue.data ?? [];

  const confirmDelete = async () => {
    if (!deleting) return;
    try {
      await rolesService.remove(deleting.id);
      addToast({ title: "Succès", description: "Rôle supprimé.", type: "success" });
      setSelectedId(undefined);
      roles.reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "Impossible de supprimer le rôle."), type: "error" });
    }
    setDeleting(null);
  };

  const isLoading = roles.isLoading || catalogue.isLoading;

  return (
    <div className="mx-auto max-w-6xl space-y-8">
      <PageHeader
        title="Rôles et permissions"
        description="Un rôle regroupe des permissions ; une permission autorise une action précise sur une ressource. Choisissez un rôle pour voir exactement ce qu'il permet."
      />

      {isLoading ? <Skeleton className="h-96 w-full" /> : (
        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[20rem_1fr]">
          <RoleRail
            roles={list}
            selectedId={selected?.id}
            totalPermissions={permissions.length}
            onSelect={setSelectedId}
            onCreate={can(PERMISSIONS.ROLES_CREATE) ? () => setEditing("new") : undefined}
          />
          {selected && (
            <RoleDetail
              role={selected}
              catalogue={permissions}
              members={canListUsers ? (users.data ?? []).filter((u) => u.roles.some((r) => r.id === selected.id)) : null}
              onEdit={() => setEditing(selected)}
              onDelete={() => setDeleting(selected)}
            />
          )}
        </div>
      )}

      {editing && (
        <RoleFormDialog
          role={editing === "new" ? null : editing}
          catalogue={permissions}
          onClose={() => setEditing(null)}
          onSaved={(saved) => { setEditing(null); setSelectedId(saved.id); roles.reload(); }}
        />
      )}

      <ConfirmDialog
        isOpen={deleting !== null}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        title="Supprimer ce rôle ?"
        description={`Le rôle ${deleting?.name ?? ""} sera supprimé définitivement. Impossible tant qu'il est attribué à quelqu'un.`}
        confirmText="Supprimer le rôle"
        cancelText="Annuler"
        destructive
      />
    </div>
  );
}

export function RolesPage() {
  return (
    <RequirePermission permission={PERMISSIONS.ROLES_READ}>
      <RolesContent />
    </RequirePermission>
  );
}
