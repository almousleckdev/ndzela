"use client";

import { rangeOf, type DateRange, type RangePreset } from "@/lib/date-range";
import { cn } from "@/lib/utils";

const CHOICES: { key: Exclude<RangePreset, "custom">; label: string }[] = [
  { key: "today", label: "Aujourd'hui" },
  { key: "yesterday", label: "Hier" },
  { key: "last7", label: "Semaine (7 jours)" },
  { key: "last30", label: "Mois (30 jours)" },
  { key: "last6Months", label: "6 mois" },
  { key: "last12Months", label: "Année (12 mois)" },
  { key: "all", label: "Toute la période" },
];

const today = () => new Date().toLocaleDateString("sv-SE");

interface ExportPeriodProps {
  value: DateRange;
  onChange: (range: DateRange) => void;
}

// The period of an export: a quick choice (day, week, month, six months, year) or any two dates.
export function ExportPeriod({ value, onChange }: ExportPeriodProps) {
  const custom = (patch: Partial<DateRange>) => onChange({ ...value, ...patch, preset: "custom" });
  const dateField = "h-9 rounded-md border border-slate-200 bg-white px-2 text-sm focus:border-primary focus:outline-none";
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2" role="group" aria-label="Périodes rapides">
        {CHOICES.map((choice) => (
          <button key={choice.key} type="button" aria-pressed={value.preset === choice.key} onClick={() => onChange(rangeOf(choice.key))}
            className={cn("rounded-full border px-3 py-1.5 text-sm", value.preset === choice.key ? "border-primary bg-primary/10 font-medium text-primary-ink" : "border-slate-200 text-slate-700 hover:bg-slate-50")}>
            {choice.label}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="text-slate-500">Ou du</span>
        <input type="date" aria-label="Du" className={dateField} max={value.to || today()} value={value.from} onChange={(e) => custom({ from: e.target.value })} />
        <span className="text-slate-500">au</span>
        <input type="date" aria-label="Au" className={dateField} min={value.from || undefined} max={today()} value={value.to} onChange={(e) => custom({ to: e.target.value })} />
      </div>
    </div>
  );
}
