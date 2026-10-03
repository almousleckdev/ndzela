"use client";

import { Check, ChevronDown } from "lucide-react";
import { Popover } from "@/components/ui/popover";
import { cn } from "@/lib/utils";

export interface FilterOption {
  value: string;
  label: string;
  count?: number;
}

interface FilterPillProps {
  label: string;
  // The empty value ("") means "no filter on this field".
  value: string;
  options: FilterOption[];
  onChange: (value: string) => void;
}

// A single-choice filter shown as a pill
export function FilterPill({ label, value, options, onChange }: FilterPillProps) {
  const selected = options.find((option) => option.value === value);
  return (
    <Popover label={`Filtrer par ${label.toLowerCase()}`} panelClassName="min-w-52 p-1"
      triggerClassName={cn(selected && "border-primary/50 bg-primary/10 text-primary-ink hover:border-primary")}
      trigger={<>{selected ? <span><span className="font-normal opacity-70">{label} : </span>{selected.label}</span> : label}<ChevronDown className="h-3.5 w-3.5 opacity-60" /></>}>
      {(close) => (
        <ul role="listbox" aria-label={label}>
          {[{ value: "", label: "Tous" }, ...options].map((option) => {
            const active = option.value === value;
            return (
              <li key={option.value || "all"}>
                <button type="button" role="option" aria-selected={active} onClick={() => { onChange(option.value); close(); }}
                  className={cn("flex w-full items-center justify-between gap-3 rounded px-2.5 py-1.5 text-left text-sm text-ink hover:bg-slate-50", active && "font-medium")}>
                  <span className="flex items-center gap-2"><span className="flex h-4 w-4 items-center justify-center">{active && <Check className="h-4 w-4 text-primary" />}</span>{option.label}</span>
                  {"count" in option && option.count !== undefined && <span className="text-xs tabular-nums text-muted">{option.count}</span>}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </Popover>
  );
}
