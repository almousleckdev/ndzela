import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import type { DriverOption } from "@/features/free-charging/types";

interface DriverOptionRowProps {
  option: DriverOption;
  checked: boolean;
  onToggle: () => void;
  disabled?: boolean;
}

export function DriverOptionRow({ option, checked, onToggle, disabled }: DriverOptionRowProps) {
  const blocked = option.inProgram ? "Déjà dans le programme" : option.linked ? null : "Compte Zella non lié";
  const car = option.fleetNumber ? `${option.fleetNumber}${option.plate ? ` · ${option.plate}` : ""}` : "Sans voiture";

  return (
    <li className="flex items-start justify-between gap-3 border-b border-line px-3 py-2 last:border-b-0">
      <Checkbox label={option.fullName} description={`${option.phone ?? "Sans téléphone"}${option.email ? ` · ${option.email}` : ""}`} checked={checked} onChange={onToggle} disabled={disabled || blocked !== null} />
      <div className="flex shrink-0 flex-col items-end gap-1">
        <span className="text-xs font-medium text-ink">{car}</span>
        {blocked && <Badge variant="warning">{blocked}</Badge>}
      </div>
    </li>
  );
}
