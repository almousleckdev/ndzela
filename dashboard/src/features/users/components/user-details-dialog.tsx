"use client";

import { ShieldAlert, ShieldCheck } from "lucide-react";
import { Avatar } from "@/components/ui/avatar";
import { Dialog } from "@/components/ui/dialog";
import { Status, type Tone } from "@/components/data-table/cells";
import { RoleChip } from "@/features/access/components/role-chip";
import { displayName } from "@/features/profile/lib/user";
import { CountryFlag } from "@/features/tenancy/components/country-flag";
import { STATUS_META } from "@/features/users/components/user-status-badge";
import type { UserStatus, UserSummary } from "@/features/users/types";

const TONE: Record<UserStatus, Tone> = { ACTIVE: "good", MFA_SETUP: "watch", LOCKED: "bad", DISABLED: "neutral" };

interface UserDetailsDialogProps {
  user: UserSummary;
  onClose: () => void;
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <div className="mt-1 text-sm text-slate-800">{children}</div>
    </div>
  );
}

// Read-only, for anyone who can see the users list (even without permission to edit, e.g. VIEWER or GESTIONNAIRE).
export function UserDetailsDialog({ user, onClose }: UserDetailsDialogProps) {
  return (
    <Dialog isOpen onClose={onClose} title="Détails de l'utilisateur" className="max-w-lg">
      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <Avatar name={displayName(user)} src={user.profilePictureUrl} size="md" />
          <div>
            <p className="font-medium text-slate-900">{displayName(user)}</p>
            <p className="text-sm text-slate-500">{user.email}</p>
          </div>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Téléphone">{user.phoneNumber ?? "—"}</Field>
          <Field label="Pays">{user.country ? <span className="inline-flex items-center gap-2"><CountryFlag code={user.country.code} />{user.country.name}</span> : "Global"}</Field>
          <Field label="Statut">
            <Status tone={TONE[user.status]}>{STATUS_META[user.status].label}</Status>
            {user.statusReason && <p className="mt-1 text-xs text-slate-500">{user.statusReason}</p>}
          </Field>
          <Field label="Authentificateur">
            <span className={user.mfaEnabled ? "inline-flex items-center gap-1.5" : "inline-flex items-center gap-1.5 text-muted"}>
              {user.mfaEnabled ? <ShieldCheck className="h-4 w-4 text-emerald-600" /> : <ShieldAlert className="h-4 w-4" />}
              {user.mfaEnabled ? "Configuré" : "Pas encore"}
            </span>
          </Field>
          <Field label="Créé le">{new Date(user.createdAt).toLocaleDateString("fr-FR")}</Field>
        </div>
        <Field label="Rôles">
          <div className="flex flex-wrap gap-1.5">{user.roles.map((role) => <RoleChip key={role.id} role={role.name} />)}</div>
        </Field>
      </div>
    </Dialog>
  );
}
