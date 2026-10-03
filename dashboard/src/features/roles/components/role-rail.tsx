import { Lock, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { roleLabel } from "@/features/access/lib/permissions";
import { roleTone } from "@/features/access/lib/role-style";
import { cn } from "@/lib/utils";
import type { RoleSummary } from "@/features/roles/types";

interface RoleRailProps {
  roles: RoleSummary[];
  selectedId: string | undefined;
  totalPermissions: number;
  onSelect: (id: string) => void;
  onCreate?: () => void;
}

// Left-hand list of roles.
export function RoleRail({ roles, selectedId, totalPermissions, onSelect, onCreate }: RoleRailProps) {
  return (
    <nav aria-label="Rôles" className="space-y-2">
      {roles.map((role) => {
        const isSelected = role.id === selectedId;
        const tone = roleTone(role.name);
        return (
          <button
            key={role.id}
            type="button"
            aria-current={isSelected}
            onClick={() => onSelect(role.id)}
            className={cn(
              "w-full rounded-xl border bg-white p-4 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
              isSelected ? cn("border-transparent shadow-md ring-2", tone.ring) : "border-slate-200 hover:border-slate-300 hover:shadow-sm"
            )}
          >
            <div className="flex items-center gap-2">
              <span className={cn("h-2.5 w-2.5 rounded-full", tone.dot)} />
              <span className="font-display text-base font-semibold text-slate-900">{roleLabel(role.name)}</span>
              {role.system && <Lock className="h-3.5 w-3.5 text-slate-400" aria-label="Rôle intégré" />}
              <span className="ml-auto text-xs tabular-nums text-slate-500">{role.userCount} {role.userCount > 1 ? "personnes" : "personne"}</span>
            </div>
            <div className="mt-3 flex items-center gap-3">
              <div className="flex flex-1 gap-0.5" aria-hidden>
                {Array.from({ length: totalPermissions }, (_, i) => (
                  <span key={i} className={cn("h-1.5 flex-1 rounded-full", i < role.permissions.length ? tone.dot : "bg-slate-200")} />
                ))}
              </div>
              <span className="text-xs font-medium tabular-nums text-slate-600">{role.permissions.length}/{totalPermissions} permissions</span>
            </div>
          </button>
        );
      })}
      {onCreate && (
        <Button variant="outline" onClick={onCreate} className="w-full border-dashed">
          <Plus className="mr-2 h-4 w-4" /> Nouveau rôle
        </Button>
      )}
    </nav>
  );
}
