import { cn } from "@/lib/utils";

interface FilterTabsProps<T extends string> {
  label: string;
  value: T;
  onChange: (value: T) => void;
  options: { key: T; label: string; count?: number }[];
}

/** Pill-shaped single-choice filter with optional counts. */
export function FilterTabs<T extends string>({ label, value, onChange, options }: FilterTabsProps<T>) {
  return (
    <div role="tablist" aria-label={label} className="flex flex-wrap gap-1">
      {options.map((option) => {
        const isActive = option.key === value;
        return (
          <button
            key={option.key}
            type="button"
            role="tab"
            aria-selected={isActive}
            onClick={() => onChange(option.key)}
            className={cn(
              "rounded-full px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
              isActive ? "bg-primary/10 text-primary" : "text-slate-600 hover:bg-slate-100"
            )}
          >
            {option.label}
            {option.count !== undefined && <span className={cn("ml-1.5 tabular-nums", isActive ? "text-primary/70" : "text-slate-400")}>{option.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
