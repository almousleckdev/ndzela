"use client";

import { useState } from "react";
import { ShieldCheck, ShieldAlert } from "lucide-react";
import { Avatar } from "@/components/ui/avatar";
import { EmptyState } from "@/components/ui/empty-state";
import { FilterTabs } from "@/components/ui/filter-tabs";
import { SearchInput } from "@/components/ui/search-input";
import { RoleChip } from "@/features/access/components/role-chip";
import { Table, Td, Th } from "@/components/ui/table";
import { displayName } from "@/features/profile/lib/user";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/features/auth/store/auth.store";
import type { UserStatus, UserSummary } from "@/features/users/types";
import { UserActionsMenu, type UserAction } from "@/features/users/components/user-actions-menu";
import { STATUS_META, UserStatusBadge } from "@/features/users/components/user-status-badge";

type Filter = "ALL" | UserStatus;
const FILTERS: { key: Filter; label: string }[] = [
  { key: "ALL", label: "Tous" },
  { key: "ACTIVE", label: "Actifs" },
  { key: "MFA_SETUP", label: "Invités" },
  { key: "LOCKED", label: "Verrouillés" },
  { key: "DISABLED", label: "Désactivés" },
];

interface UsersTableProps {
  users: UserSummary[];
  onAction: (action: UserAction, user: UserSummary) => void;
  onInvite?: () => void;
}

export function UsersTable({ users, onAction, onInvite }: UsersTableProps) {
  const [filter, setFilter] = useState<Filter>("ALL");
  const [query, setQuery] = useState("");
  const myEmail = useAuthStore((state) => state.user?.email);

  const count = (key: Filter) => (key === "ALL" ? users.length : users.filter((u) => u.status === key).length);
  const needle = query.trim().toLowerCase();
  const visible = users
    .filter((u) => filter === "ALL" || u.status === filter)
    .filter((u) => !needle || `${displayName(u)} ${u.email}`.toLowerCase().includes(needle));

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
        <FilterTabs
          label="Filtrer par statut"
          value={filter}
          onChange={setFilter}
          options={FILTERS.map(({ key, label }) => ({ key, label, count: count(key) }))}
        />
        <SearchInput label="Rechercher un utilisateur" placeholder="Nom ou email" value={query} onChange={(e) => setQuery(e.target.value)} className="w-full sm:w-64" />
      </div>

      {visible.length === 0 ? (
        <div className="p-6">
          <EmptyState
            title={users.length === 0 ? "Aucun utilisateur pour l'instant" : `Aucun résultat${filter !== "ALL" ? ` parmi les comptes « ${STATUS_META[filter].label.toLowerCase()} »` : ""}`}
            description={users.length === 0 && onInvite ? "Invitez votre premier administrateur pour commencer." : "Essayez un autre filtre ou une autre recherche."}
          />
        </div>
      ) : (
        <Table>
          <thead>
            <tr><Th>Utilisateur</Th><Th>Rôles</Th><Th>Statut</Th><Th>Authentificateur</Th><Th><span className="sr-only">Actions</span></Th></tr>
          </thead>
          <tbody>
            {visible.map((user) => (
              <tr key={user.id} className="transition-colors hover:bg-slate-50/70">
                <Td>
                  <div className="flex items-center gap-3">
                    <Avatar name={displayName(user)} src={user.profilePictureUrl} />
                    <div className="min-w-0">
                      <div className="truncate font-medium text-slate-900">{displayName(user)}</div>
                      <div className="truncate text-xs text-slate-500">{user.email}</div>
                    </div>
                  </div>
                </Td>
                <Td><div className="flex flex-wrap gap-1.5">{user.roles.map((role) => <RoleChip key={role.id} role={role.name} />)}</div></Td>
                <Td><UserStatusBadge status={user.status} /></Td>
                <Td>
                  <span className={cn("inline-flex items-center gap-1.5 text-sm", user.mfaEnabled ? "text-slate-700" : "text-slate-400")}>
                    {user.mfaEnabled ? <ShieldCheck className="h-4 w-4 text-emerald-600" /> : <ShieldAlert className="h-4 w-4" />}
                    {user.mfaEnabled ? "Configuré" : "Pas encore"}
                  </span>
                </Td>
                <Td className="text-right"><UserActionsMenu user={user} isSelf={user.email === myEmail} onAction={onAction} /></Td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}
