import { DriverGratuityBadge } from "@/features/free-charging/components/driver-gratuity-badge";
import { isChecked, isToggleable, toggle, toggleAll, type Selection } from "@/features/free-charging/lib/selection";
import type { DriverOption } from "@/features/free-charging/types";

interface DriverTableProps {
  drivers: DriverOption[];
  selection: Selection;
  onChange: (selection: Selection) => void;
  disabled: boolean;
}

const STATUSES: Record<string, string> = { ACTIVE: "Actif", SUSPENDED: "Suspendu", BLOCKED: "Bloqué", INACTIVE: "En pause", ARCHIVED: "Démissionnaire" };

const carOf = (driver: DriverOption) => [driver.fleetNumber, driver.plate].filter(Boolean).join(" · ") || "Sans voiture";

export function DriverTable({ drivers, selection, onChange, disabled }: DriverTableProps) {
  const toggleable = drivers.filter(isToggleable);
  const allChecked = toggleable.length > 0 && toggleable.every((driver) => isChecked(driver, selection));

  return (
    <div className="overflow-x-auto rounded-lg border border-line">
      <table className="w-full min-w-[640px] text-left text-sm">
        <caption className="sr-only">Tous les chauffeurs</caption>
        <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>
            <th scope="col" className="w-12 px-3 py-2">
              <input type="checkbox" aria-label="Sélectionner les chauffeurs de la page" className="h-4 w-4 cursor-pointer accent-primary" checked={allChecked}
                disabled={disabled || toggleable.length === 0} onChange={() => onChange(toggleAll(drivers, selection, !allChecked))} />
            </th>
            <th scope="col" className="px-3 py-2">Chauffeur</th>
            <th scope="col" className="px-3 py-2">Voiture</th>
            <th scope="col" className="px-3 py-2">Statut</th>
            <th scope="col" className="px-3 py-2">Programme</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {drivers.map((driver) => (
            <tr key={driver.driverId} className={isToggleable(driver) ? "hover:bg-slate-50" : "text-slate-500"}>
              <td className="px-3 py-2.5">
                <input type="checkbox" aria-label={`Gratuité de ${driver.fullName}`} className="h-4 w-4 cursor-pointer accent-primary disabled:cursor-not-allowed"
                  checked={isChecked(driver, selection)} disabled={disabled || !isToggleable(driver)} onChange={() => onChange(toggle(driver, selection))} />
              </td>
              <td className="px-3 py-2.5">
                <p className="font-medium text-ink">{driver.fullName}</p>
                <p className="text-xs text-slate-500">{driver.phone ?? "Sans téléphone"}{driver.email ? ` · ${driver.email}` : ""}</p>
              </td>
              <td className="px-3 py-2.5">{carOf(driver)}</td>
              <td className="px-3 py-2.5">{STATUSES[driver.status] ?? driver.status}</td>
              <td className="px-3 py-2.5"><DriverGratuityBadge driver={driver} selection={selection} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
