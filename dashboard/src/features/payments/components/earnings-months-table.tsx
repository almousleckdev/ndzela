"use client";

import { useMemo } from "react";
import { format } from "date-fns";
import { fr } from "date-fns/locale";
import { EmptyState } from "@/components/ui/empty-state";
import { DataTable } from "@/components/data-table/data-table";
import { columnsFor } from "@/components/data-table/features";
import { Money } from "@/components/data-table/cells";
import type { EarningsMonth } from "@/features/payments/types";

const col = columnsFor<EarningsMonth>();

// "2026-11" -> "novembre 2026"
const monthLabel = (month: string) => {
  const [year, number] = month.split("-").map(Number);
  return format(new Date(year, number - 1, 1), "LLLL yyyy", { locale: fr });
};

// Month by month, from the first payment to now
export function EarningsMonthsTable({ months }: { months: EarningsMonth[] | null }) {
  const columns = useMemo(() => col.columns([
    col.accessor("month", { id: "month", header: "Mois", cell: (c) => <span className="capitalize">{monthLabel(c.getValue())}</span> }),
    col.accessor("collected", { id: "collected", header: "Encaissé", cell: (c) => <Money value={c.getValue()} /> }),
    col.accessor("fees", { id: "fees", header: "Frais opérateur", cell: (c) => <Money value={c.getValue()} /> }),
    col.accessor("net", { id: "net", header: "Net", cell: (c) => <Money value={c.getValue()} /> }),
  ]), []);
  return <DataTable columns={columns} data={months} getRowId={(row) => row.month} numeric={["collected", "fees", "net"]} initialSorting={[{ id: "month", desc: true }]} empty={<EmptyState title="Aucun paiement enregistré." />} />;
}
