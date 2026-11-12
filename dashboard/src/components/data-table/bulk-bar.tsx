"use client";

import { Button } from "@/components/ui/button";

// The strip shown above a table while rows are selected.
export function BulkBar({ count, onClear, children }: { count: number; onClear: () => void; children: React.ReactNode }) {
  if (count === 0) return null;
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-line bg-orange-50/60 px-4 py-2 text-sm">
      <span className="font-medium text-ink">{count} sélectionné{count > 1 ? "s" : ""}</span>
      <div className="flex gap-2">
        <Button variant="outline" onClick={onClear}>Tout désélectionner</Button>
        {children}
      </div>
    </div>
  );
}
