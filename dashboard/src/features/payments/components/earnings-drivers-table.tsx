"use client";

import { useMemo } from "react";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { columnsFor } from "@/components/data-table/features";
import { Money, None, Stack, Status } from "@/components/data-table/cells";
import { formatDateTime } from "@/lib/time";
import type { EarningsDriver } from "@/features/payments/types";

const col = columnsFor<EarningsDriver>();

// What each driver paid, current and former, the biggest first
export function EarningsDriversTable({ rows, error, onRetry }: { rows: EarningsDriver[] | null; error?: unknown; onRetry?: () => void }) {
  const columns = useMemo(() => col.columns([
    col.accessor("name", { id: "name", header: "Chauffeur", cell: (c) => <Stack primary={c.getValue()} secondary={c.row.original.former ? <Status tone="neutral">Ancien</Status> : undefined} /> }),
    col.accessor("contractNumber", { id: "contractNumber", header: "Contrat", cell: (c) => c.getValue() ?? <None /> }),
    col.accessor("totalPaid", { id: "totalPaid", header: "Total payé", cell: (c) => <Money value={c.getValue()} /> }),
    col.accessor("lastPayment", { id: "lastPayment", header: "Dernier paiement", cell: (c) => (c.getValue() ? formatDateTime(c.getValue() as string) : <None />) }),
  ]), []);
  return <DataTable columns={columns} data={rows} error={error} onRetry={onRetry} getRowId={(row) => row.driverId} numeric={["totalPaid"]} initialSorting={[{ id: "totalPaid", desc: true }]} empty={<EmptyState title="Aucun paiement enregistré." />} />;
}
