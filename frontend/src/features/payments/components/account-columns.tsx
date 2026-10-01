"use client";

import { useMemo } from "react";
import Link from "next/link";
import { Eye, HandCoins, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import { None, Stack, Status, type Tone } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import type { AccountRow, ArrearsLevel } from "@/features/payments/types";

const col = columnsFor<AccountRow>();
export const LEVEL_TONE: Record<ArrearsLevel, Tone> = { NONE: "good", LATE: "neutral", YELLOW: "watch", DARK_YELLOW: "alert", RED: "bad" };
/** The 3 px marker on a row's left edge. */
export const LEVEL_MARKER: Record<ArrearsLevel, string | undefined> = { NONE: undefined, LATE: "bg-slate-400", YELLOW: "bg-yellow-400", DARK_YELLOW: "bg-orange-500", RED: "bg-red-600" };
const STATE = { SUSPENDED: "Suspendu", BLOCKED: "Bloqué", INACTIVE: "Inactif" } as Record<string, string>;

interface Options {
  onPay?: (row: AccountRow) => void;
  onWaive?: (row: AccountRow) => void;
  /** Show the "collected in the period" column. */
  showCollected: boolean;
}

/** The columns of the follow-up tables (Suivi paiements and Gérer impayés), which differ only in the period column and the actions. */
export function useAccountColumns({ onPay, onWaive, showCollected }: Options) {
  return useMemo(() => col.columns([
    col.accessor("driverName", { id: "driverName", header: "Chauffeur", cell: (c) => (
      <Stack primary={<Link href={`/drivers/${c.row.original.driverId}`} className="hover:text-primary-ink">{c.getValue()}</Link>} secondary={[c.row.original.phone, STATE[c.row.original.driverStatus]].filter(Boolean).join(" · ")} />
    ) }),
    col.accessor("vehicle", { id: "vehicle", header: "Véhicule", enableSorting: false, cell: (c) => c.getValue() ?? <span className="text-muted">Aucun</span> }),
    col.accessor("dailyRent", { id: "dailyRent", header: "Loyer", enableSorting: false, cell: (c) => (c.getValue() ? formatFcfa(c.getValue() as number) : <span className="text-muted">Pas de contrat</span>) }),
    col.accessor("overdueAmount", { id: "overdueAmount", header: "Montant dû", cell: (c) => <span className={c.getValue() > 0 ? "font-semibold" : "text-muted"}>{formatFcfa(c.getValue())}</span> }),
    col.accessor("overdueSince", { id: "overdueSince", header: "En retard depuis", cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    ...(showCollected ? [col.accessor("collected", { id: "collected", header: "Encaissé sur la période", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : formatFcfa(c.getValue() as number)) })] : []),
    col.accessor("lastPaymentAt", { id: "lastPaymentAt", header: "Dernier paiement", enableSorting: false, cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    col.accessor("level", { id: "level", header: "Niveau", enableSorting: false, cell: (c) => <Status tone={LEVEL_TONE[c.getValue()]}>{LEVEL_LABELS[c.getValue()]}</Status> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const row = c.row.original;
      return (
        <div className="flex justify-end gap-0.5">
          <Link href={`/drivers/${row.driverId}`}><Button variant="ghost" size="icon" aria-label={`Voir ${row.driverName}`} title="Voir le chauffeur"><Eye className="h-4 w-4" /></Button></Link>
          {onPay && row.contractId && <Button variant="ghost" size="icon" aria-label={`Enregistrer un paiement pour ${row.driverName}`} title="Enregistrer un paiement" onClick={() => onPay(row)}><Wallet className="h-4 w-4" /></Button>}
          {onWaive && row.contractId && row.overdueAmount > 0 && <Button variant="ghost" size="icon" aria-label={`Accorder un délai à ${row.driverName}`} title="Accorder un délai" onClick={() => onWaive(row)}><HandCoins className="h-4 w-4" /></Button>}
        </div>
      );
    } }),
  ]), [onPay, onWaive, showCollected]);
}
