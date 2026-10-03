"use client";

import { useMemo } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { ActiveFilters, type ActiveFilter } from "@/components/data-table/active-filters";
import { None, Stack } from "@/components/data-table/cells";
import { DataTable } from "@/components/data-table/data-table";
import { DateRangePicker } from "@/components/data-table/date-range-picker";
import { columnsFor } from "@/components/data-table/features";
import { SearchField } from "@/components/data-table/search-field";
import { useTableQuery } from "@/components/data-table/use-table-query";
import { Toolbar } from "@/components/layout/page";
import { useAsync } from "@/hooks/use-async";
import { describeRange, NO_RANGE, rangeOf } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { chargingService } from "@/features/charging/services/charging.service";
import type { ChargingSessionRow } from "@/features/charging/types";

const col = columnsFor<ChargingSessionRow>();

function duration(row: ChargingSessionRow): string | null {
  if (!row.startedAt || !row.endedAt) return null;
  const minutes = Math.max(0, Math.round((new Date(row.endedAt).getTime() - new Date(row.startedAt).getTime()) / 60000));
  return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, "0")}`;
}

// The recharges reported by the OCPP platform; the amount is its own calculation (energy times tariff, before VAT), not a payment.
export function SessionsTab() {
  const query = useTableQuery({ range: rangeOf("last7") });
  const { range } = query;
  const list = useAsync(() => chargingService.sessions({ q: query.q, from: range.from, to: range.to }, query.page, query.size), [query.q, range.from, range.to, query.page, query.size]);

  const columns = useMemo(() => col.columns([
    col.accessor("startedAt", { id: "startedAt", header: "Début", enableSorting: false, cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    col.display({ id: "station", header: "Borne", cell: (c) => <Stack primary={c.row.original.chargePointId ?? <None />} secondary={c.row.original.connectorId ? `Connecteur ${c.row.original.connectorId}` : undefined} /> }),
    col.accessor("idTag", { id: "idTag", header: "Identifiant", enableSorting: false, cell: (c) => <span className="font-mono text-xs">{c.getValue() ?? "-"}</span> }),
    col.accessor("energyKwh", { id: "energyKwh", header: "Énergie", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : <span className="whitespace-nowrap tabular-nums">{c.getValue()} kWh</span>) }),
    col.display({ id: "duration", header: "Durée", cell: (c) => duration(c.row.original) ?? <None /> }),
    col.accessor("amount", { id: "amount", header: "Montant calculé", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : formatFcfa(c.getValue() as number)) }),
    col.display({ id: "soc", header: "Batterie", cell: (c) => { const r = c.row.original; return r.socBefore === null && r.socAfter === null ? <None /> : <span className="whitespace-nowrap tabular-nums">{r.socBefore ?? "?"} % → {r.socAfter ?? "?"} %</span>; } }),
    col.accessor("status", { id: "status", header: "Statut", enableSorting: false, cell: (c) => <Stack primary={c.getValue() ?? <None />} secondary={c.row.original.stopReason ?? undefined} /> }),
    col.accessor("transactionId", { id: "transactionId", header: "Référence", enableSorting: false, cell: (c) => <span className="font-mono text-xs">{c.getValue()}</span> }),
  ]), []);

  const active: ActiveFilter[] = [
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <>
      <p className="mb-4 text-sm text-muted">Les recharges de la station, telles que la plateforme de recharge les transmet. Le montant calculé (énergie × tarif, avant TVA) n&apos;est pas la preuve d&apos;un paiement.</p>
      <Toolbar start={(
        <>
          <SearchField label="Rechercher une session" placeholder="Borne, identifiant ou référence" value={query.search} onChange={query.setSearch} />
          <DateRangePicker value={range} onChange={query.setRange} />
        </>
      )} />
      <ActiveFilters filters={active} onClear={query.reset} />
      <DataTable columns={columns} data={list.data?.items ?? null} error={list.error} onRetry={list.reload} getRowId={(row) => row.id} numeric={["energyKwh", "amount"]}
        server={{ total: list.data?.total ?? 0, page: query.page, size: query.size, onPageChange: query.setPage, onSizeChange: query.setSize, sorting: [], onSortingChange: () => {} }}
        empty={<EmptyState title="Aucune session" description="Aucune session de recharge reçue sur cette période." />} />
    </>
  );
}
