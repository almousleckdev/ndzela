"use client";

import { toOptions } from "@/lib/options";
import { useMemo } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { Stack, Status } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { FilterPill } from "@/components/data-table/filter-pill";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Page, PageHead, Panel, Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { describeRange, NO_RANGE } from "@/lib/date-range";
import { formatDateTime } from "@/lib/time";
import { RequirePermission } from "@/features/access/components/require-permission";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { useAuthStore } from "@/features/auth/store/auth.store";
import { ACTION_LABELS, TARGET_TYPE_LABELS, actionLabel, targetTypeLabel } from "@/features/audit/lib/audit-labels";
import { auditService } from "@/features/audit/services/audit.service";
import type { AuditLog } from "@/features/audit/types";
import { CountryFlag } from "@/features/tenancy/components/country-flag";

const col = columnsFor<AuditLog>();
const ACTION_OPTIONS = toOptions(ACTION_LABELS);
const TARGET_OPTIONS = toOptions(TARGET_TYPE_LABELS);

function AuditContent() {
  const isGlobal = useAuthStore((state) => state.user?.country === null);
  const query = useTableQuery({ sorting: [{ id: "createdAt", desc: true }] });
  const { filters, range } = query;
  const list = useAsync(
    () => auditService.search({ action: filters.action ?? "", actor: query.q, targetType: filters.targetType ?? "", from: range.from, to: range.to }, query.page, query.size, query.sort),
    [filters.action, filters.targetType, query.q, range.from, range.to, query.page, query.size, query.sort],
  );

  const columns = useMemo(() => col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Date", cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.accessor("actor", { id: "actor", header: "Acteur", cell: (c) => <span className="break-all">{c.getValue()}</span> }),
    col.accessor("action", { id: "action", header: "Action", cell: (c) => <Stack primary={actionLabel(c.getValue())} secondary={c.row.original.details ?? c.row.original.reason} /> }),
    col.accessor("target", { id: "target", header: "Cible", enableSorting: false, cell: (c) => <Stack primary={c.getValue()} secondary={targetTypeLabel(c.row.original.targetType)} /> }),
    ...(isGlobal ? [col.display({ id: "country", header: "Pays", cell: (c) => c.row.original.country ? <span className="inline-flex items-center gap-2"><CountryFlag code={c.row.original.country.code} />{c.row.original.country.name}</span> : <span className="text-muted">Global</span> })] : []),
    col.accessor("result", { id: "result", header: "Résultat", cell: (c) => <Status tone={c.getValue() === "SUCCESS" ? "good" : "bad"}>{c.getValue() === "SUCCESS" ? "Réussi" : "Échec"}</Status> }),
  ]), [isGlobal]);

  const active: ActiveFilter[] = [
    ...(filters.action ? [{ key: "action", label: actionLabel(filters.action), onRemove: () => query.setFilter("action", "") }] : []),
    ...(filters.targetType ? [{ key: "target", label: targetTypeLabel(filters.targetType), onRemove: () => query.setFilter("targetType", "") }] : []),
    ...(query.search ? [{ key: "q", label: `Acteur : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <Page>
      <PageHead title="Journal d'audit" description="Qui a fait quoi, et quand. Les enregistrements ne peuvent ni être modifiés ni supprimés." />
      <Panel>
        <Toolbar start={(
          <>
            <SearchField label="Rechercher un acteur" placeholder="Rechercher un acteur (email)" value={query.search} onChange={query.setSearch} />
            <FilterPill label="Action" value={filters.action ?? ""} options={ACTION_OPTIONS} onChange={(value) => query.setFilter("action", value)} />
            <FilterPill label="Cible" value={filters.targetType ?? ""} options={TARGET_OPTIONS} onChange={(value) => query.setFilter("targetType", value)} />
            <DateRangePicker value={range} onChange={query.setRange} />
          </>
        )} />
        <ActiveFilters filters={active} onClear={query.reset} />
        <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(log) => log.id}
          server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: query.sorting, onSortingChange: query.setSorting }}
          empty={<EmptyState title="Aucun enregistrement" description="Aucune action ne correspond à ces filtres. Élargissez la période ou effacez un filtre." />} />
      </Panel>
    </Page>
  );
}

export function AuditPage() {
  return (
    <RequirePermission permission={PERMISSIONS.AUDIT_READ}>
      <AuditContent />
    </RequirePermission>
  );
}
