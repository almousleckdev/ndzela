import { addDays, endOfMonth, format, startOfMonth, startOfYear, subDays, subMonths } from "date-fns";

export type RangePreset = "today" | "yesterday" | "last7" | "last30" | "last6Months" | "last12Months" | "thisMonth" | "lastMonth" | "thisYear" | "all" | "custom";

// Inclusive days as yyyy-MM-dd; an empty string leaves that side open.
export interface DateRange {
  from: string;
  to: string;
  preset: RangePreset;
}

export const PRESETS: { key: Exclude<RangePreset, "custom">; label: string }[] = [
  { key: "today", label: "Aujourd'hui" },
  { key: "yesterday", label: "Hier" },
  { key: "last7", label: "7 derniers jours" },
  { key: "last30", label: "30 derniers jours" },
  { key: "last6Months", label: "6 derniers mois" },
  { key: "last12Months", label: "12 derniers mois" },
  { key: "thisMonth", label: "Ce mois" },
  { key: "lastMonth", label: "Mois dernier" },
  { key: "thisYear", label: "Cette année" },
  { key: "all", label: "Toute la période" },
];

const iso = (date: Date) => format(date, "yyyy-MM-dd");

export const NO_RANGE: DateRange = { from: "", to: "", preset: "all" };

// The range a preset stands for, counted from `today` (the user's local day).
export function rangeOf(preset: Exclude<RangePreset, "custom">, today: Date = new Date()): DateRange {
  switch (preset) {
    case "today": return { from: iso(today), to: iso(today), preset };
    case "yesterday": return { from: iso(subDays(today, 1)), to: iso(subDays(today, 1)), preset };
    case "last7": return { from: iso(subDays(today, 6)), to: iso(today), preset };
    case "last30": return { from: iso(subDays(today, 29)), to: iso(today), preset };
    case "last6Months": return { from: iso(addDays(subMonths(today, 6), 1)), to: iso(today), preset };
    case "last12Months": return { from: iso(addDays(subMonths(today, 12), 1)), to: iso(today), preset };
    case "thisMonth": return { from: iso(startOfMonth(today)), to: iso(today), preset };
    case "lastMonth": {
      const previous = subMonths(today, 1);
      return { from: iso(startOfMonth(previous)), to: iso(endOfMonth(previous)), preset };
    }
    case "thisYear": return { from: iso(startOfYear(today)), to: iso(today), preset };
    case "all": return NO_RANGE;
  }
}

const parse = (value: string) => new Date(`${value}T00:00:00`);
const short = (value: string) => format(parse(value), "d MMM yyyy").replace(".", "");

// A short label for the button and the filter chip.
export function describeRange(range: DateRange): string {
  if (range.preset !== "custom") return PRESETS.find((p) => p.key === range.preset)?.label ?? "Toute la période";
  if (range.from && range.to) return range.from === range.to ? short(range.from) : `${short(range.from)} au ${short(range.to)}`;
  if (range.from) return `Depuis le ${short(range.from)}`;
  return range.to ? `Jusqu'au ${short(range.to)}` : "Toute la période";
}

export const toDate = parse;
export const toIso = iso;
