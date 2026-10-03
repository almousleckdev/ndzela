"use client";

import { useState } from "react";
import { Plus } from "lucide-react";
import { RequirePermission } from "@/features/access/components/require-permission";
import { UserFormDialog } from "@/features/users/components/user-form-dialog";
import type { UserAction } from "@/features/users/components/user-actions-menu";
import { UsersTable } from "@/features/users/components/users-table";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { KpiBand, Page, PageHead, Panel } from "@/components/layout/page";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { useFetch } from "@/hooks/use-fetch";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { displayName } from "@/features/profile/lib/user";
import { rolesService } from "@/features/roles/services/roles.service";
import { usersService } from "@/features/users/services/users.service";
import type { UserSummary } from "@/features/users/types";

type ConfirmableAction = "suspend" | "block" | "delete" | "reset-mfa";
type Confirmation = { action: ConfirmableAction; user: UserSummary };

const CONFIRMATIONS: Record<ConfirmableAction, { title: string; text: (name: string) => string; confirm: string; success: string; run: (id: string, reason: string) => Promise<unknown> }> = {
  suspend: {
    title: "Suspendre cet utilisateur ?",
    text: (n) => `${n} ne pourra plus se connecter et ses sessions seront fermées. Vous pourrez le réactiver à tout moment.`,
    confirm: "Suspendre",
    success: "Utilisateur suspendu.",
    run: usersService.disable,
  },
  block: {
    title: "Bloquer cet utilisateur ?",
    text: (n) => `${n} sera bloqué et déconnecté. Le blocage reste actif jusqu'à ce que vous le débloquiez.`,
    confirm: "Bloquer",
    success: "Utilisateur bloqué.",
    run: usersService.lock,
  },
  delete: {
    title: "Supprimer cet utilisateur ?",
    text: (n) => `Le compte de ${n} sera supprimé définitivement. Pour une absence temporaire, préférez « Suspendre ».`,
    confirm: "Supprimer définitivement",
    success: "Utilisateur supprimé.",
    run: usersService.remove,
  },
  "reset-mfa": {
    title: "Réinitialiser l'authentificateur ?",
    text: (n) => `${n} devra configurer une nouvelle application d'authentification à sa prochaine connexion.`,
    confirm: "Réinitialiser",
    success: "Authentificateur réinitialisé.",
    run: usersService.resetMfa,
  },
};

function UsersContent() {
  const { can } = usePermissions();
  const { addToast } = useToast();
  const users = useFetch(usersService.list);
  const roles = useFetch(rolesService.list);
  const [editing, setEditing] = useState<UserSummary | "new" | null>(null);
  const [confirming, setConfirming] = useState<Confirmation | null>(null);

  // Runs an admin action, reports the result and refreshes the list.
  const run = async (request: () => Promise<unknown>, success: string) => {
    try {
      await request();
      addToast({ title: "Succès", description: success, type: "success" });
      users.reload();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
    }
  };

  const handleAction = (action: UserAction, user: UserSummary) => {
    switch (action) {
      case "edit": return setEditing(user);
      case "suspend":
      case "block":
      case "delete":
      case "reset-mfa": return setConfirming({ action, user });
      case "reactivate": return run(() => usersService.enable(user.id), "Utilisateur réactivé.");
      case "unblock": return run(() => usersService.unlock(user.id), "Utilisateur débloqué.");
      case "resend-invitation": return run(() => usersService.resendInvitation(user.id), "Invitation renvoyée.");
    }
  };

  const confirmed = async () => {
    if (!confirming) return;
    const { action, user } = confirming;
    setConfirming(null);
    await run(() => CONFIRMATIONS[action].run(user.id, ""), CONFIRMATIONS[action].success);
  };

  const copy = confirming ? CONFIRMATIONS[confirming.action] : null;
  const needsReason = confirming?.action === "suspend" || confirming?.action === "block";

  const confirmedWithReason = async (reason: string) => {
    if (!confirming || !copy) return;
    await copy.run(confirming.user.id, reason);
    setConfirming(null);
    addToast({ title: "Succès", description: copy.success, type: "success" });
    users.reload();
  };

  return (
    <Page>
      <PageHead
        title="Utilisateurs"
        description="Qui a accès à NDZela Admin, avec quel rôle, et dans quel état est son compte."
        actions={can(PERMISSIONS.USERS_CREATE) && (
          <Button onClick={() => setEditing("new")} disabled={!roles.data}>
            <Plus className="mr-2 h-4 w-4" /> Inviter un utilisateur
          </Button>
        )}
      />
      <KpiBand items={[
        { label: "Utilisateurs", value: users.data?.length ?? "-" },
        { label: "Actifs", value: users.data ? users.data.filter((u) => u.status === "ACTIVE").length : "-" },
        { label: "Invitations en attente", value: users.data ? users.data.filter((u) => u.status === "MFA_SETUP").length : "-" },
        { label: "Verrouillés ou désactivés", value: users.data ? users.data.filter((u) => u.status === "LOCKED" || u.status === "DISABLED").length : "-" },
      ]} />

      <Panel>
        {users.isLoading
          ? <Skeleton className="h-64 w-full rounded-none" />
          : <UsersTable users={users.data ?? []} onAction={handleAction} onInvite={can(PERMISSIONS.USERS_CREATE) ? () => setEditing("new") : undefined} />}
      </Panel>

      {editing && roles.data && (
        <UserFormDialog
          user={editing === "new" ? null : editing}
          roles={roles.data}
          onClose={() => setEditing(null)}
          onSaved={() => { setEditing(null); users.reload(); }}
        />
      )}

      {confirming && copy && needsReason && (
        <ReasonDialog title={copy.title} description={copy.text(displayName(confirming.user))} confirmText={copy.confirm} destructive
          onClose={() => setConfirming(null)} onConfirm={confirmedWithReason} />
      )}
      <ConfirmDialog
        isOpen={confirming !== null && !needsReason}
        onClose={() => setConfirming(null)}
        onConfirm={confirmed}
        title={copy?.title ?? ""}
        description={confirming && copy ? copy.text(displayName(confirming.user)) : ""}
        confirmText={copy?.confirm}
        cancelText="Annuler"
        destructive
      />
    </Page>
  );
}

export function UsersPage() {
  return (
    <RequirePermission permission={PERMISSIONS.USERS_READ}>
      <UsersContent />
    </RequirePermission>
  );
}
