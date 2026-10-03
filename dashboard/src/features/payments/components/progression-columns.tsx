"use client";

import Link from "next/link";
import { None, Stack } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { ContractProgress, UnpaidDays } from "@/features/payments/components/day-cells";
import type { AccountRow } from "@/features/payments/types";

const col = columnsFor<AccountRow>();

// The Progression tab of the history
export const progressionColumns = col.columns([
  col.accessor("driverName", { id: "driverName", header: "Chauffeur", cell: (c) => (
    <Stack primary={<Link href={`/drivers/${c.row.original.driverId}`} className="hover:text-primary-ink">{c.getValue()}</Link>} secondary={c.row.original.phone} />
  ) }),
  col.accessor("vehicle", { id: "vehicle", header: "Véhicule", enableSorting: false, cell: (c) => c.getValue() ?? <span className="text-muted">Aucun</span> }),
  col.display({ id: "progress", header: "Progression", cell: (c) => <ContractProgress elapsed={c.row.original.elapsedDays} total={c.row.original.totalDays} /> }),
  col.accessor("paidDays", { id: "paidDays", header: "Jours payés", enableSorting: false }),
  col.accessor("unpaidDays", { id: "unpaidDays", header: "Impayés", enableSorting: false, cell: (c) => <UnpaidDays days={c.getValue()} /> }),
  col.accessor("overdueAmount", { id: "overdueAmount", header: "Montant dû", cell: (c) => <span className={c.getValue() > 0 ? "font-semibold" : "text-muted"}>{formatFcfa(c.getValue())}</span> }),
  col.accessor("lastPaymentAt", { id: "lastPaymentAt", header: "Dernier paiement", enableSorting: false, cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
]);
