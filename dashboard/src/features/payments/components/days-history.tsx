"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { formatDateTime } from "@/lib/time";
import { cn } from "@/lib/utils";
import { DAY_STATUS_CLASS, DAY_STATUS_LABELS, formatRentDay } from "@/features/payments/lib/payment-labels";
import type { DayView } from "@/features/payments/types";

// The last week is always shown; the whole history opens and closes below it.
const LAST_WEEK = 7;

export function DaysHistory({ days }: { days: DayView[] }) {
  const [open, setOpen] = useState(false);
  if (days.length === 0) return <p className="text-sm text-slate-500">Aucun loyer échu pour l&apos;instant.</p>;
  const shown = open ? days : days.slice(0, LAST_WEEK);

  return (
    <div>
      <ul className={cn("grid grid-cols-1 gap-1.5 sm:grid-cols-2", open && "max-h-96 overflow-y-auto pr-1")}>
        {shown.map((d) => (
          <li key={d.day} className="flex items-center justify-between rounded-md border border-slate-100 px-3 py-1.5 text-sm">
            <span>{formatRentDay(d.day)}{open && ` ${d.day.slice(0, 4)}`}{d.settledAt && <span className="ml-2 text-xs text-slate-500">payé le {formatDateTime(d.settledAt)}</span>}</span>
            <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", DAY_STATUS_CLASS[d.status])}>{DAY_STATUS_LABELS[d.status]}</span>
          </li>
        ))}
      </ul>
      {days.length > LAST_WEEK && (
        <Button variant="ghost" size="sm" className="mt-2" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
          {open ? "Voir moins" : `Voir plus (${days.length - LAST_WEEK} jours)`}
        </Button>
      )}
    </div>
  );
}
