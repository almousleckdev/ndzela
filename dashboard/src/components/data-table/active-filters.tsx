import { X } from "lucide-react";

export interface ActiveFilter {
  key: string;
  label: string;
  onRemove: () => void;
}

// The filters currently applied, each removable, so nobody wonders why a table looks short.
export function ActiveFilters({ filters, onClear }: { filters: ActiveFilter[]; onClear: () => void }) {
  if (filters.length === 0) return null;
  return (
    <div className="flex flex-wrap items-center gap-2 px-4 pb-3 text-sm" aria-label="Filtres actifs">
      <span className="text-xs text-muted">Filtres appliqués</span>
      {filters.map((filter) => (
        <span key={filter.key} className="inline-flex items-center gap-1 rounded-full bg-slate-100 py-0.5 pl-2.5 pr-1 text-xs font-medium text-ink">
          {filter.label}
          <button type="button" aria-label={`Retirer le filtre ${filter.label}`} onClick={filter.onRemove} className="flex h-5 w-5 items-center justify-center rounded-full text-muted hover:bg-slate-200 hover:text-ink"><X className="h-3 w-3" /></button>
        </span>
      ))}
      <button type="button" onClick={onClear} className="text-xs font-medium text-primary-ink hover:underline">Tout effacer</button>
    </div>
  );
}
