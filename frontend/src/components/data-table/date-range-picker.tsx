"use client";

import { useState } from "react";
import { CalendarDays } from "lucide-react";
import { DayPicker, type DateRange as PickerRange } from "react-day-picker";
import { fr } from "react-day-picker/locale";
import { Popover } from "@/components/ui/popover";
import { PRESETS, describeRange, rangeOf, toDate, toIso, type DateRange } from "@/lib/date-range";
import { cn } from "@/lib/utils";

interface DateRangePickerProps {
  value: DateRange;
  onChange: (range: DateRange) => void;
  label?: string;
}

/** Quick periods on the left (last 7 days, this month...), a two-click calendar on the right for anything else. */
export function DateRangePicker({ value, onChange, label = "Période" }: DateRangePickerProps) {
  const active = value.preset !== "all";
  return (
    <Popover label={label} align="start" panelClassName="flex w-[min(92vw,34rem)] flex-col sm:flex-row"
      triggerClassName={cn(active && "border-primary/50 bg-orange-50 text-primary-ink hover:border-primary")}
      trigger={<><CalendarDays className="h-4 w-4 opacity-70" /><span>{active ? describeRange(value) : label}</span></>}>
      {(close) => <RangePanel value={value} onPick={(range) => { onChange(range); close(); }} />}
    </Popover>
  );
}

function RangePanel({ value, onPick }: { value: DateRange; onPick: (range: DateRange) => void }) {
  const [draft, setDraft] = useState<PickerRange | undefined>(value.from || value.to ? { from: value.from ? toDate(value.from) : undefined, to: value.to ? toDate(value.to) : undefined } : undefined);

  return (
    <>
      <ul className="flex shrink-0 flex-row flex-wrap gap-1 border-b border-line p-2 sm:w-44 sm:flex-col sm:flex-nowrap sm:border-b-0 sm:border-r" aria-label="Périodes rapides">
        {PRESETS.map((preset) => (
          <li key={preset.key}>
            <button type="button" aria-pressed={value.preset === preset.key} onClick={() => onPick(rangeOf(preset.key))}
              className={cn("w-full rounded px-2.5 py-1.5 text-left text-sm text-ink hover:bg-slate-50", value.preset === preset.key && "bg-orange-50 font-medium text-primary-ink")}>
              {preset.label}
            </button>
          </li>
        ))}
      </ul>
      <div className="p-2">
        <DayPicker mode="range" locale={fr} weekStartsOn={1} numberOfMonths={1} selected={draft} defaultMonth={draft?.from ?? new Date()} disabled={{ after: new Date() }}
          onSelect={(range) => {
            setDraft(range);
            if (range?.from && range?.to) onPick({ from: toIso(range.from), to: toIso(range.to), preset: "custom" });
          }} />
        <p className="px-2 pb-1 text-xs text-muted">Choisissez le premier puis le dernier jour.</p>
      </div>
    </>
  );
}
