"use client";

import { useMemo } from "react";
import Link from "next/link";
import { CloudLightning, Eye, HandCoins, MessageCircle, Moon, Wallet } from "lucide-react";
import { Button } from "@/components/ui/button";
import { None, Stack, Status, type Tone } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { whatsappLink } from "@/lib/whatsapp";
import { DaysLeft, UnpaidDays } from "@/features/payments/components/day-cells";
import { LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import type { AccountRow, ArrearsLevel, IncidentKind } from "@/features/payments/types";

const col = columnsFor<AccountRow>();
export const LEVEL_TONE: Record<ArrearsLevel, Tone> = { NONE: "good", LATE: "neutral", YELLOW: "watch", DARK_YELLOW: "bad", RED: "critical" };
// The 3 px marker on a row's left edge.
export const LEVEL_MARKER: Record<ArrearsLevel, string | undefined> = { NONE: undefined, LATE: "bg-slate-400", YELLOW: "bg-yellow-400", DARK_YELLOW: "bg-red-500", RED: "bg-red-900" };
const STATE = { SUSPENDED: "Suspendu", BLOCKED: "Bloqué", INACTIVE: "Inactif" } as Record<string, string>;

interface Options {
  onPay?: (row: AccountRow) => void;
  onWaive?: (row: AccountRow) => void;
  // Declare a half day or a stop for a driver (administrators only).
  onIncident?: (row: AccountRow, kind: IncidentKind) => void;
  // Show a WhatsApp link to contact the driver.
  contact?: boolean;
  // Show the "collected in the period" column.
  showCollected: boolean;
}

// The columns of the follow-up tables (Suivi paiements and Gérer impayés)
export function useAccountColumns({ onPay, onWaive, onIncident, contact, showCollected }: Options) {
  return useMemo(() => col.columns([
    col.accessor("driverName", { id: "driverName", header: "Chauffeur", cell: (c) => (
      <Stack primary={<Link href={`/drivers/${c.row.original.driverId}`} className="hover:text-primary-ink">{c.getValue()}</Link>} secondary={[c.row.original.phone, STATE[c.row.original.driverStatus]].filter(Boolean).join(" · ")} />
    ) }),
    col.accessor("vehicle", { id: "vehicle", header: "Véhicule", enableSorting: false, cell: (c) => c.getValue() ?? <span className="text-muted">Aucun</span> }),
    col.accessor("dailyRent", { id: "dailyRent", header: "Loyer", enableSorting: false, cell: (c) => (c.getValue() ? formatFcfa(c.getValue() as number) : <span className="text-muted">Pas de contrat</span>) }),
    col.accessor("unpaidDays", { id: "unpaidDays", header: "Jours impayés", enableSorting: false, cell: (c) => <UnpaidDays days={c.getValue()} /> }),
    col.accessor("daysLeft", { id: "daysLeft", header: "Jours restants", enableSorting: false, cell: (c) => <DaysLeft days={c.getValue()} hasContract={Boolean(c.row.original.contractId)} /> }),
    col.accessor("overdueAmount", { id: "overdueAmount", header: "Montant dû", cell: (c) => <span className={c.getValue() > 0 ? "font-semibold" : "text-muted"}>{formatFcfa(c.getValue())}</span> }),
    col.accessor("awaitingAmount", { id: "awaitingAmount", header: "À régler avant midi", enableSorting: false, cell: (c) => (c.getValue() > 0 ? <span className="whitespace-nowrap">{formatFcfa(c.getValue())}</span> : <None />) }),
    col.accessor("overdueSince", { id: "overdueSince", header: "En retard depuis", cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    ...(showCollected ? [col.accessor("collected", { id: "collected", header: "Encaissé sur la période", enableSorting: false, cell: (c) => (c.getValue() === null ? <None /> : formatFcfa(c.getValue() as number)) })] : []),
    col.accessor("lastPaymentAt", { id: "lastPaymentAt", header: "Dernier paiement", enableSorting: false, cell: (c) => (c.getValue() ? <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue() as string)}</span> : <None />) }),
    col.accessor("level", { id: "level", header: "Niveau", enableSorting: false, cell: (c) => <Status tone={LEVEL_TONE[c.getValue()]}>{LEVEL_LABELS[c.getValue()]}</Status> }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const row = c.row.original;
      return (
        <div className="flex justify-end gap-0.5">
          <Link href={`/drivers/${row.driverId}`}><Button variant="ghost" size="icon" aria-label={`Voir ${row.driverName}`} title="Voir le chauffeur"><Eye className="h-4 w-4" /></Button></Link>
          {contact && row.phone && <a href={whatsappLink(row.phone)} target="_blank" rel="noopener noreferrer" aria-label={`Écrire à ${row.driverName} sur WhatsApp`} title="Contacter sur WhatsApp"><Button variant="ghost" size="icon" tabIndex={-1}><MessageCircle className="h-4 w-4 text-green-600" /></Button></a>}
          {onPay && row.contractId && <Button variant="ghost" size="icon" aria-label={`Enregistrer un paiement pour ${row.driverName}`} title="Enregistrer un paiement" onClick={() => onPay(row)}><Wallet className="h-4 w-4" /></Button>}
          {onWaive && row.contractId && row.overdueAmount > 0 && <Button variant="ghost" size="icon" aria-label={`Accorder un délai à ${row.driverName}`} title="Accorder un délai" onClick={() => onWaive(row)}><HandCoins className="h-4 w-4" /></Button>}
          {onIncident && row.contractId && <Button variant="ghost" size="icon" aria-label={`Déclarer une demi-journée pour ${row.driverName}`} title="Déclarer une demi-journée" onClick={() => onIncident(row, "HALF_DAY")}><CloudLightning className="h-4 w-4" /></Button>}
          {onIncident && row.contractId && <Button variant="ghost" size="icon" aria-label={`Déclarer un arrêt pour ${row.driverName}`} title="Déclarer un arrêt" onClick={() => onIncident(row, "STOP")}><Moon className="h-4 w-4" /></Button>}
        </div>
      );
    } }),
  ]), [onPay, onWaive, onIncident, contact, showCollected]);
}
