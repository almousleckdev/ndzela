"use client";

import { Stack, Status } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { reasonLabel } from "@/features/payments/lib/suspension-case-labels";
import type { PatternCase } from "@/features/payments/types";

const col = columnsFor<PatternCase>();

// An account whose charges look like two cars, and the drivers suspended for arrears at the same time
export const patternCaseColumns = col.columns([
  col.display({ id: "account", header: "Compte", cell: (c) => <Stack primary={c.row.original.account.name} secondary={c.row.original.account.vehicle ?? undefined} /> }),
  col.display({ id: "signals", header: "Indices", cell: (c) => <Status tone="bad">{c.row.original.reasons.map(reasonLabel).join(" · ")}</Status> }),
  col.accessor("score", { id: "score", header: "Score", enableSorting: false, cell: (c) => <span className="tabular-nums">{c.getValue()} / 100</span> }),
  col.display({ id: "suspended", header: "Chauffeurs suspendus", cell: (c) => <span className="text-muted">{c.row.original.suspendedDrivers.join(", ")}</span> }),
]);
