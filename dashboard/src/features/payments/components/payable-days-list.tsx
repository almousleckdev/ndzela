import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { formatFcfa } from "@/lib/money";
import { formatRentDay } from "@/features/payments/lib/payment-labels";
import type { PayableDay } from "@/features/payments/types";

interface PayableDaysListProps {
  days: PayableDay[];
  selected: string[];
  onChange: (selected: string[]) => void;
}

// The days staff can pay, oldest first, with the total of the ticked ones
export function PayableDaysList({ days, selected, onChange }: PayableDaysListProps) {
  const total = days.filter((d) => selected.includes(d.day)).reduce((sum, d) => sum + d.amount, 0);
  const toggle = (day: string) => onChange(days.map((d) => d.day).filter((d) => (d === day ? !selected.includes(d) : selected.includes(d))));
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-slate-800">Jours à encaisser *</p>
        <Button type="button" size="sm" variant="outline" onClick={() => onChange(days.map((d) => d.day))}>Tout sélectionner</Button>
      </div>
      <div className="max-h-64 overflow-y-auto rounded-lg border border-slate-200 p-1">
        {days.map((d) => (
          <Checkbox key={d.day} label={`${formatRentDay(d.day)}${d.late ? " (en retard)" : ""}`} checked={selected.includes(d.day)} onChange={() => toggle(d.day)}
            description={d.penalty > 0 ? `${formatFcfa(d.amount)}, dont pénalité ${formatFcfa(d.penalty)}` : formatFcfa(d.amount)} />
        ))}
      </div>
      <p className="text-sm text-slate-700">Total : <strong>{formatFcfa(total)}</strong></p>
    </div>
  );
}
