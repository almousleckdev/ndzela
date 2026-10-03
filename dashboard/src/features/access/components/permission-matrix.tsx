import { Check, Lock } from "lucide-react";
import { groupPermissions, isGrantable, permissionLabel } from "@/features/access/lib/permissions";
import { cn } from "@/lib/utils";

interface PermissionMatrixProps {
  // Every permission that exists (defines the rows and the chips in each row).
  catalogue: string[];
  granted: string[];
  // When set, chips become toggles.
  onToggle?: (permission: string) => void;
  // Permissions the signed-in admin already holds
  held?: string[];
}

// The access grid
export function PermissionMatrix({ catalogue, granted, onToggle, held }: PermissionMatrixProps) {
  return (
    <div className="space-y-2">
      {groupPermissions(catalogue).map((group) => (
        <div key={group.resource} className="flex flex-col gap-2 rounded-lg bg-slate-50 px-3 py-2.5 sm:flex-row sm:items-center sm:gap-4">
          <div className="w-28 shrink-0 text-sm font-medium text-slate-800">{group.label}</div>
          <div className="flex flex-wrap gap-1.5">
            {group.permissions.map((permission) => (
              <Chip
                key={permission}
                permission={permission}
                on={granted.includes(permission)}
                label={`${permissionLabel(permission)} ${group.label}`}
                onToggle={onToggle}
                locked={onToggle !== undefined && held !== undefined && !isGrantable(permission, held)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

interface ChipProps {
  permission: string;
  on: boolean;
  label: string;
  onToggle?: (permission: string) => void;
  locked: boolean;
}

function Chip({ permission, on, label, onToggle, locked }: ChipProps) {
  const face = (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium transition-colors",
        on ? "bg-primary text-white shadow-sm" : "border border-slate-200 bg-white text-slate-400",
        onToggle && !locked && !on && "hover:border-primary/50 hover:text-slate-600",
        locked && "opacity-50"
      )}
    >
      {on ? <Check className="h-3 w-3" strokeWidth={3} /> : locked ? <Lock className="h-3 w-3" /> : null}
      {permissionLabel(permission)}
    </span>
  );

  if (!onToggle) return <span aria-label={`${label} : ${on ? "autorisé" : "non autorisé"}`} role="img">{face}</span>;
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      aria-label={label}
      title={locked ? "Vous ne possédez pas cette permission d'administration" : permission}
      disabled={locked}
      onClick={() => onToggle(permission)}
      className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed"
    >
      {face}
    </button>
  );
}
