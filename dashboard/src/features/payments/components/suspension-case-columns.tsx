"use client";

import { None, Stack, Status } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { helperLabel, scoreLabel } from "@/features/payments/lib/suspension-case-labels";
import type { SuspensionCase } from "@/features/payments/types";

const col = columnsFor<SuspensionCase>();

// What was refused for a suspended driver, who helped (when we know) and whether the helper's charges look like two cars
export const suspensionCaseColumns = col.columns([
  col.accessor("occurredAt", { id: "occurredAt", header: "Date", enableSorting: false, cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
  col.display({ id: "suspended", header: "Chauffeur suspendu", cell: (c) => c.row.original.suspended ? <Stack primary={c.row.original.suspended.name} secondary={c.row.original.suspended.vehicle ?? undefined} /> : <None /> }),
  col.display({ id: "helper", header: "Compte utilisé", cell: (c) => { const label = helperLabel(c.row.original); return <Stack primary={label.primary} secondary={label.secondary} />; } }),
  col.accessor("amountMinor", { id: "amountMinor", header: "Montant", enableSorting: false, cell: (c) => <span className="tabular-nums">{formatFcfa(c.getValue())}</span> }),
  col.display({ id: "signals", header: "Indices", cell: (c) => <Status tone={c.row.original.flagged ? "bad" : "neutral"}>{scoreLabel(c.row.original)}</Status> }),
]);
