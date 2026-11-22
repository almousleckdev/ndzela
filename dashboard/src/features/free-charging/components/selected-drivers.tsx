import { X } from "lucide-react";
import type { DriverOption } from "@/features/free-charging/types";

interface SelectedDriversProps {
  drivers: DriverOption[];
  onRemove: (driver: DriverOption) => void;
  onClear: () => void;
  disabled?: boolean;
}

export function SelectedDrivers({ drivers, onRemove, onClear, disabled }: SelectedDriversProps) {
  if (drivers.length === 0) return null;
  return (
    <div className="space-y-2 rounded-lg border border-primary bg-slate-50 p-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-medium text-ink">{drivers.length === 1 ? "1 chauffeur choisi" : `${drivers.length} chauffeurs choisis`}</p>
        <button type="button" onClick={onClear} disabled={disabled} className="text-xs font-medium text-primary hover:underline disabled:opacity-50">Tout désélectionner</button>
      </div>
      <ul className="flex flex-wrap gap-2">
        {drivers.map((driver) => (
          <li key={driver.driverId} className="inline-flex items-center gap-1 rounded-full border border-line bg-white py-0.5 pl-3 pr-1 text-xs text-ink">
            {driver.fullName}
            <button type="button" aria-label={`Retirer ${driver.fullName} de la sélection`} onClick={() => onRemove(driver)} disabled={disabled}
              className="grid h-5 w-5 place-items-center rounded-full text-muted hover:bg-slate-100 disabled:opacity-50"><X className="h-3 w-3" /></button>
          </li>
        ))}
      </ul>
    </div>
  );
}
