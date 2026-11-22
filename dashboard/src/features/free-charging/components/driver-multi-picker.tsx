"use client";

import { useState } from "react";
import { SearchField } from "@/components/data-table/search-field";
import { useAsync } from "@/hooks/use-async";
import { useDebounced } from "@/hooks/use-debounced";
import { getErrorMessage } from "@/lib/errors";
import { DriverOptionRow } from "@/features/free-charging/components/driver-option-row";
import { SelectedDrivers } from "@/features/free-charging/components/selected-drivers";
import { freeChargingService } from "@/features/free-charging/services/free-charging.service";
import type { DriverOption } from "@/features/free-charging/types";

interface DriverMultiPickerProps {
  selected: DriverOption[];
  onChange: (selected: DriverOption[]) => void;
  error?: string;
  disabled?: boolean;
}

const pickable = (option: DriverOption) => option.linked && !option.inProgram;

export function DriverMultiPicker({ selected, onChange, error, disabled }: DriverMultiPickerProps) {
  const [search, setSearch] = useState("");
  const q = useDebounced(search);
  const drivers = useAsync(() => freeChargingService.drivers(q), [q]);
  const items = drivers.data ?? [];
  const chosen = new Set(selected.map((driver) => driver.driverId));
  const open = items.filter((option) => pickable(option) && !chosen.has(option.driverId));

  const toggle = (option: DriverOption) => onChange(chosen.has(option.driverId) ? selected.filter((driver) => driver.driverId !== option.driverId) : [...selected, option]);

  return (
    <fieldset className="space-y-3" disabled={disabled}>
      <legend className="mb-1.5 text-sm font-medium text-ink">Chauffeurs à ajouter</legend>
      <SelectedDrivers drivers={selected} onRemove={toggle} onClear={() => onChange([])} disabled={disabled} />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <SearchField value={search} onChange={setSearch} label="Rechercher un chauffeur" placeholder="Nom, téléphone, e-mail ou n° de voiture (V015)" className="sm:w-80" />
        {open.length > 0 && <button type="button" onClick={() => onChange([...selected, ...open])} className="text-xs font-medium text-primary hover:underline">Tout sélectionner</button>}
      </div>
      {drivers.error != null && <p role="alert" className="text-sm font-medium text-red-600">{getErrorMessage(drivers.error, "Impossible de charger les chauffeurs.")}</p>}
      {!drivers.isLoading && drivers.error == null && items.length === 0 && <p className="text-sm text-muted">Aucun chauffeur trouvé.</p>}
      {items.length > 0 && (
        <ul aria-label="Chauffeurs à ajouter" className="max-h-64 overflow-y-auto rounded-lg border border-line">
          {items.map((option) => <DriverOptionRow key={option.driverId} option={option} checked={chosen.has(option.driverId)} onToggle={() => toggle(option)} disabled={disabled} />)}
        </ul>
      )}
      {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
    </fieldset>
  );
}
