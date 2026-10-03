"use client";

import { cn } from "@/lib/utils";

export interface TabItem<T extends string> {
  value: T;
  label: string;
}

interface TabsProps<T extends string> {
  tabs: TabItem<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
}

// Tabs for switching between views of the same page
export function Tabs<T extends string>({ tabs, value, onChange, label }: TabsProps<T>) {
  return (
    <div role="tablist" aria-label={label} className="flex gap-1 border-b border-slate-200">
      {tabs.map((tab) => {
        const selected = tab.value === value;
        return (
          <button key={tab.value} role="tab" type="button" aria-selected={selected} onClick={() => onChange(tab.value)}
            className={cn("-mb-px border-b-2 px-4 py-2.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40",
              selected ? "border-primary text-ink" : "border-transparent text-muted hover:text-ink")}>
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
