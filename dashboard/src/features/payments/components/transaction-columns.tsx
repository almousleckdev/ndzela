"use client";

import { Button } from "@/components/ui/button";
import { PaidDays } from "@/features/payments/components/paid-days";
import { None, Stack, Status } from "@/components/data-table/cells";
import { columnsFor } from "@/components/data-table/features";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { METHOD_LABELS, TRANSACTION_STATUS_LABELS, TRANSACTION_STATUS_TONE } from "@/features/payments/lib/payment-labels";
import type { TransactionRow } from "@/features/payments/types";

const col = columnsFor<TransactionRow>();

// The Transactions tab
export function transactionColumns(onReverse?: (row: TransactionRow) => void, onReceipt?: (row: TransactionRow) => void) {
  return col.columns([
    col.accessor("createdAt", { id: "createdAt", header: "Date et heure", enableSorting: false, cell: (c) => <span className="whitespace-nowrap tabular-nums">{formatDateTime(c.getValue())}</span> }),
    col.accessor("driverName", { id: "driverName", header: "Chauffeur", enableSorting: false, cell: (c) => <Stack primary={c.getValue()} secondary={c.row.original.phone ?? undefined} /> }),
    col.accessor("amount", { id: "amount", header: "Montant", enableSorting: false, cell: (c) => <span className={c.row.original.status === "SUCCESS" ? "font-medium" : "text-muted"}>{formatFcfa(c.getValue())}</span> }),
    col.accessor("method", { id: "method", header: "Méthode", enableSorting: false, cell: (c) => METHOD_LABELS[c.getValue()] }),
    col.display({ id: "days", header: "Jours réglés", enableSorting: false, cell: (c) => <PaidDays days={c.row.original.days} paidAt={c.row.original.paidAt} /> }),
    col.accessor("status", { id: "status", header: "Statut", enableSorting: false, cell: (c) => <Status tone={TRANSACTION_STATUS_TONE[c.getValue()]}>{TRANSACTION_STATUS_LABELS[c.getValue()]}</Status> }),
    col.accessor("reference", { id: "reference", header: "Référence", enableSorting: false, cell: (c) => (c.getValue() ? <span className="block max-w-[14rem] truncate font-mono text-xs" title={c.getValue()}>{c.getValue()}</span> : <None />) }),
    col.display({ id: "actions", header: () => <span className="sr-only">Actions</span>, enableHiding: false, cell: (c) => {
      const row = c.row.original;
      const recorded = row.status === "SUCCESS" || row.status === "REVERSED";
      return (
        <div className="flex justify-end gap-1">
          {onReceipt && recorded && <Button variant="ghost" size="sm" onClick={() => onReceipt(row)}>Reçu</Button>}
          {onReverse && row.source === "MANUAL" && row.status === "SUCCESS" && <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" onClick={() => onReverse(row)}>Annuler</Button>}
        </div>
      );
    } }),
  ]);
}
