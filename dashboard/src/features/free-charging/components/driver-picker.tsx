"use client";

import { useState } from "react";
import { SearchField } from "@/components/data-table/search-field";
import { Button } from "@/components/ui/button";
import { useAsync } from "@/hooks/use-async";
import { useDebounced } from "@/hooks/use-debounced";
import { getErrorMessage } from "@/lib/errors";
import { DriverTable } from "@/features/free-charging/components/driver-table";
import { DRIVER_PAGE_SIZE, pendingCounts, type Membership, type Selection } from "@/features/free-charging/lib/selection";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";

interface DriverPickerProps {
  selection: Selection;
  onChange: (selection: Selection) => void;
  onClear: () => void;
  disabled: boolean;
}

const FILTERS: { value: Membership; label: string }[] = [
  { value: "ALL", label: "Tous" },
  { value: "IN_PROGRAM", label: "Dans le programme" },
  { value: "NOT_IN_PROGRAM", label: "Hors programme" },
];

export function DriverPicker({ selection, onChange, onClear, disabled }: DriverPickerProps) {
  const [search, setSearch] = useState("");
  const [membership, setMembership] = useState<Membership>("ALL");
  const [page, setPage] = useState(0);
  const q = useDebounced(search);
  const drivers = useAsync(() => freeChargingService.drivers(q, membership, page), [q, membership, page]);
  const counts = pendingCounts(selection);
  const pending = counts.add + counts.remove;
  const items = drivers.data?.items ?? [];
  const total = drivers.data?.total ?? 0;
  const lastPage = Math.max(0, Math.ceil(total / DRIVER_PAGE_SIZE) - 1);

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SearchField value={search} onChange={(value) => { setSearch(value); setPage(0); }} label="Rechercher un chauffeur" placeholder="Nom, téléphone, e-mail ou n° de voiture (V015)" className="sm:w-80" />
        <div role="group" aria-label="Afficher" className="inline-flex gap-0.5 rounded-lg border border-line p-0.5">
          {FILTERS.map((filter) => (
            <button key={filter.value} type="button" aria-pressed={membership === filter.value} onClick={() => { setMembership(filter.value); setPage(0); }}
              className={`h-8 whitespace-nowrap rounded-md px-3 text-sm font-medium ${membership === filter.value ? "bg-primary/10 text-primary" : "text-muted hover:bg-slate-100"}`}>
              {filter.label}
            </button>
          ))}
        </div>
      </div>
      {drivers.error != null && <p role="alert" className="text-sm font-medium text-red-600">{getErrorMessage(drivers.error, "Impossible de charger les chauffeurs.")}</p>}
      {!drivers.isLoading && drivers.error == null && items.length === 0 && <p className="text-sm text-muted">Aucun chauffeur trouvé.</p>}
      {items.length > 0 && <DriverTable drivers={items} selection={selection} onChange={onChange} disabled={disabled} />}
      {total > DRIVER_PAGE_SIZE && (
        <nav aria-label="Pagination" className="flex items-center justify-between text-xs text-muted">
          <p>{`${page * DRIVER_PAGE_SIZE + 1}–${Math.min(total, (page + 1) * DRIVER_PAGE_SIZE)} sur ${total}`}</p>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setPage(page - 1)} disabled={page === 0}>Précédent</Button>
            <Button variant="outline" onClick={() => setPage(page + 1)} disabled={page >= lastPage}>Suivant</Button>
          </div>
        </nav>
      )}
      <div role="status" className="flex flex-wrap items-center justify-between gap-2 text-sm">
        <p className="text-muted">{pending === 0 ? "Cochez un chauffeur pour l'ajouter, décochez un chauffeur du programme pour le retirer." : `${counts.add} à ajouter · ${counts.remove} à retirer`}</p>
        {pending > 0 && <button type="button" onClick={onClear} disabled={disabled} className="text-sm font-medium text-primary hover:underline">Annuler les changements</button>}
      </div>
    </div>
  );
}
