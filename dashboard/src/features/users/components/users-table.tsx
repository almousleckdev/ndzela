"use client";

import { useMemo, useState } from "react";
import { ShieldAlert, ShieldCheck } from "lucide-react";
import { format } from "date-fns";
import { Avatar } from "@/components/ui/avatar";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { Stack, Status, type Tone } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { Toolbar } from "@/components/layout/page";
import { describeRange, NO_RANGE, type DateRange } from "@/lib/date-range";
import { RoleChip } from "@/features/access/components/role-chip";
import { roleLabel } from "@/features/access/lib/permissions";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { displayName } from "@/features/profile/lib/user";
import { CountryFlag } from "@/features/tenancy/components/country-flag";
import { UserActionsMenu, type UserAction } from "@/features/users/components/user-actions-menu";
import { STATUS_META } from "@/features/users/components/user-status-badge";
import type { UserStatus, UserSummary } from "@/features/users/types";

const TONE: Record<UserStatus, Tone> = { ACTIVE: "good", MFA_SETUP: "watch", LOCKED: "bad", DISABLED: "neutral" };
const col = columnsFor<UserSummary>();

interface UsersTableProps {
  users: UserSummary[];
  onAction: (action: UserAction, user: UserSummary) => void;
  onInvite?: () => void;
}

// The staff list is small, so it is searched, filtered, sorted and paged in the browser.
export function UsersTable({ users, onAction, onInvite }: UsersTableProps) {
  const myEmail = useAuthStore((state) => state.user?.email);
  const showCountryColumn = useAuthStore((state) => state.user?.isGlobal || (state.user?.countries.length ?? 0) > 1);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [role, setRole] = useState("");
  const [range, setRange] = useState<DateRange>(NO_RANGE);

  const visible = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return users.filter((user) => {
      if (status && user.status !== status) return false;
      if (role && !user.roles.some((r) => r.name === role)) return false;
      if (needle && !`${displayName(user)} ${user.email}`.toLowerCase().includes(needle)) return false;
      const day = format(new Date(user.createdAt), "yyyy-MM-dd");
      return (!range.from || day >= range.from) && (!range.to || day <= range.to);
    });
  }, [users, search, status, role, range]);

  const columns = useMemo(() => col.columns([
    col.accessor((u) => displayName(u), { id: "name", header: "Utilisateur", cell: (c) => (
      <div className="flex items-center gap-3"><Avatar name={c.getValue()} src={c.row.original.profilePictureUrl} size="sm" /><Stack primary={c.getValue()} secondary={c.row.original.email} /></div>
    ) }),
    col.accessor((u) => u.roles.map((r) => r.name).join(", "), { id: "roles", header: "Rôles", enableSorting: false, cell: (c) => <div className="flex flex-wrap gap-1.5">{c.row.original.roles.map((r) => <RoleChip key={r.id} role={r.name} />)}</div> }),
    ...(showCountryColumn ? [col.accessor((u) => u.isGlobal ? "Global" : u.countries.map((c) => c.name).join(", "), { id: "country", header: "Pays", cell: (c) => {
      const row = c.row.original;
      if (row.isGlobal) return <span className="text-muted">Global</span>;
      return <div className="flex flex-wrap gap-1.5">{row.countries.map((country) => <span key={country.id} className="inline-flex items-center gap-1.5"><CountryFlag code={country.code} />{country.name}</span>)}</div>;
    } })] : []),
    col.accessor("status", { id: "status", header: "Statut", cell: (c) => <Stack primary={<Status tone={TONE[c.getValue()]}>{STATUS_META[c.getValue()].label}</Status>} secondary={c.row.original.statusReason} /> }),
    col.accessor("mfaEnabled", { id: "mfa", header: "Authentificateur", cell: (c) => (
      <span className={c.getValue() ? "inline-flex items-center gap-1.5" : "inline-flex items-center gap-1.5 text-muted"}>{c.getValue() ? <ShieldCheck className="h-4 w-4 text-emerald-600" /> : <ShieldAlert className="h-4 w-4" />}{c.getValue() ? "Configuré" : "Pas encore"}</span>
    ) }),
    col.accessor("createdAt", { id: "createdAt", header: "Créé le", cell: (c) => <span className="whitespace-nowrap tabular-nums">{new Date(c.getValue()).toLocaleDateString("fr-FR")}</span> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => <div className="flex justify-end"><UserActionsMenu user={c.row.original} isSelf={c.row.original.email === myEmail} onAction={onAction} /></div> }),
  ]), [showCountryColumn, myEmail, onAction]);

  const count = (key: UserStatus) => users.filter((user) => user.status === key).length;
  const statusOptions = (Object.keys(STATUS_META) as UserStatus[]).map((value) => ({ value, label: STATUS_META[value].label, count: count(value) }));
  const roleNames = Array.from(new Set(users.flatMap((user) => user.roles.map((r) => r.name)))).sort();
  const active: ActiveFilter[] = [
    ...(status ? [{ key: "status", label: STATUS_META[status as UserStatus].label, onRemove: () => setStatus("") }] : []),
    ...(role ? [{ key: "role", label: roleLabel(role), onRemove: () => setRole("") }] : []),
    ...(search ? [{ key: "q", label: `Recherche : ${search}`, onRemove: () => setSearch("") }] : []),
    ...(range.preset !== "all" ? [{ key: "range", label: `Créé : ${describeRange(range)}`, onRemove: () => setRange(NO_RANGE) }] : []),
  ];
  const clear = () => { setSearch(""); setStatus(""); setRole(""); setRange(NO_RANGE); };

  return (
    <div>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher un utilisateur" placeholder="Nom ou email" value={search} onChange={setSearch} />
          <FilterPill label="Statut" value={status} options={statusOptions} onChange={setStatus} />
          <FilterPill label="Rôle" value={role} options={roleNames.map((name) => ({ value: name, label: roleLabel(name) }))} onChange={setRole} />
          <DateRangePicker value={range} onChange={setRange} label="Créé le" />
        </>
      )} />
      <ActiveFilters filters={active} onClear={clear} />
      <DataTable columns={columns} data={visible} getRowId={(user) => user.id} initiallyHidden={["createdAt"]} initialSorting={[{ id: "name", desc: false }]}
        empty={<EmptyState title={users.length === 0 ? "Aucun utilisateur pour l'instant" : "Aucun résultat"} description={users.length === 0 && onInvite ? "Invitez votre premier administrateur pour commencer." : "Aucun utilisateur ne correspond à ces filtres."} />} />
    </div>
  );
}
