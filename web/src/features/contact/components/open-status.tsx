"use client";

import { useSyncExternalStore } from "react";
import { isOpenAt } from "@/lib/opening-hours";

const subscribe = (onChange: () => void) => {
  const id = setInterval(onChange, 60_000);
  return () => clearInterval(id);
};

// "Ouvert" or "Fermé" right now, read in Libreville time
export function OpenStatus() {
  const open = useSyncExternalStore(subscribe, () => isOpenAt(new Date()), () => null);
  if (open === null) return <span className="h-7" />;
  return (
    <span className={`inline-flex h-7 items-center gap-2 rounded-full px-3 text-sm font-semibold ${open ? "bg-emerald-100 text-emerald-900" : "bg-slate-200 text-slate-800"}`}>
      <span className={`h-2 w-2 rounded-full ${open ? "bg-emerald-600" : "bg-slate-500"}`} aria-hidden />
      {open ? "Ouvert maintenant" : "Fermé maintenant"}
    </span>
  );
}
