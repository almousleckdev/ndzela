"use client";

import { useMemo } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { usePolling } from "@/hooks/use-polling";
import { describeRange, NO_RANGE, rangeOf } from "@/lib/date-range";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import { batteryLabel, durationLabel, isRunning, powerLabel, reasonLabel, statusLabel } from "@/features/charging/components/session-format";
import { chargingService } from "@/features/charging/services/charging.service";
import type { ChargingSessionRow } from "@/features/charging/types";

const col = columnsFor<ChargingSessionRow>();

// A session still under way is measured up to now.
// The recharges reported by the OCPP platform; the amount is its own calculation (energy times tariff, before VAT), not a payment.
export function SessionsTab({ onRecord }: { onRecord: (session: ChargingSessionRow) => void }) {
  const { can } = usePermissions();
  const query = useTableQuery({ range: rangeOf("last7") });
  const { range } = query;
  const list = useAsync(() => chargingService.sessions({ q: query.q, from: range.from, to: range.to }, query.page, query.size), [query.q, range.from, range.to, query.page, query.size]);

  usePolling(list.reload, 5000);

  const columns = useMemo(() => col.columns([
    col.accessor("startedAt", { id: "startedAt", header: "Début", enableSorting: false, cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    col.display({ id: "station", header: "Borne", cell: (c) => <Stack primary={c.row.original.chargePointId ?? <None />} secondary={c.row.original.connectorId ? `Connecteur ${c.row.original.connectorId}` : undefined} /> }),
    col.accessor("idTag", { id: "idTag", header: "Identifiant", enableSorting: false, cell: (c) => <span className="font-mono text-xs">{c.getValue() ?? "-"}</span> }),
    col.accessor("energyKwh", { id: "energyKwh", header: "Énergie", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : <Stack primary={<span className="whitespace-nowrap tabular-nums">{c.getValue()} kWh</span>} secondary={powerLabel(c.row.original) ?? undefined} />) }),
    col.display({ id: "duration", header: "Durée", cell: (c) => <Stack primary={durationLabel(c.row.original) ?? <None />} secondary={isRunning(c.row.original) ? "en cours" : undefined} /> }),
    col.accessor("amount", { id: "amount", header: "Montant calculé", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : <Stack primary={formatFcfa(c.getValue() as number)} secondary={c.row.original.pricePerKwh !== null ? `${c.row.original.pricePerKwh} FCFA/kWh` : undefined} />) }),
    col.display({ id: "soc", header: "Batterie", cell: (c) => { const battery = batteryLabel(c.row.original); return battery ? <Stack primary={<span className="whitespace-nowrap tabular-nums">{battery.primary}</span>} secondary={battery.secondary} /> : <None />; } }),
    col.accessor("status", { id: "status", header: "Statut", enableSorting: false, cell: (c) => <Stack primary={statusLabel(c.getValue()) ?? <None />} secondary={reasonLabel(c.row.original.stopReason) ?? undefined} /> }),
    col.accessor("transactionId", { id: "transactionId", header: "Référence", enableSorting: false, cell: (c) => <span className="font-mono text-xs">{c.getValue()}</span> }),
    col.display({ id: "payment", header: "Paiement", enableHiding: false, cell: (c) => {
      const row = c.row.original;
      if (row.rechargeId) return <Badge variant="success">Payée</Badge>;
      if (isRunning(row)) return <Badge variant="info">En cours</Badge>;
      return can(PERMISSIONS.CHARGING_CREATE) ? <Button size="sm" variant="outline" onClick={() => onRecord(row)}>Enregistrer le paiement</Button> : <Badge>Non enregistrée</Badge>;
    } }),
  ]), [can, onRecord]);

  const active: ActiveFilter[] = [
    ...(query.search ? [{ key: "q", label: `Recherche : ${query.search}`, onRemove: () => query.setSearch("") }] : []),
    ...(query.rangeChanged ? [{ key: "range", label: describeRange(range), onRemove: () => query.setRange(NO_RANGE) }] : []),
  ];

  return (
    <>
      <p className="mb-4 text-sm text-muted">Les sessions de la borne, en direct, telles que la plateforme de recharge les transmet. Le montant calculé (énergie × tarif, avant TVA) n&apos;est pas la preuve d&apos;un paiement : une fois le client payé, enregistrez le paiement pour que la recharge compte dans les recettes.</p>
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
