import { Lock, Pencil, Trash2 } from "lucide-react";
import { PermissionMatrix } from "@/features/access/components/permission-matrix";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS, roleDescription, roleLabel } from "@/features/access/lib/permissions";
import { roleTone } from "@/features/access/lib/role-style";
import { displayName } from "@/features/profile/lib/user";
import { cn } from "@/lib/utils";
import type { UserSummary } from "@/features/users/types";
import type { RoleSummary } from "@/features/roles/types";

interface RoleDetailProps {
  role: RoleSummary;
  catalogue: string[];
  /** Everyone who holds this role, or null when the viewer may not list users. */
  members: UserSummary[] | null;
  onEdit: () => void;
  onDelete: () => void;
}

export function RoleDetail({ role, catalogue, members, onEdit, onDelete }: RoleDetailProps) {
  const { can } = usePermissions();
  const tone = roleTone(role.name);

  return (
    <Card className="p-0">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 p-6">
        <div className="flex items-start gap-4">
          <span className={cn("mt-1.5 h-4 w-4 shrink-0 rounded-full", tone.dot)} />
          <div>
            <h2 className="font-display text-2xl font-semibold tracking-tight text-slate-900">{roleLabel(role.name)}</h2>
            <p className="mt-0.5 font-mono text-xs text-slate-500">{role.name}</p>
            <p className="mt-2 max-w-xl text-sm leading-relaxed text-slate-600">{roleDescription(role.name)}</p>
          </div>
        </div>

        {role.system ? (
          <p className="flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-600">
            <Lock className="h-3.5 w-3.5" /> Rôle intégré : non modifiable
          </p>
        ) : (
          <div className="flex gap-2">
            {can(PERMISSIONS.ROLES_UPDATE) && <Button variant="outline" onClick={onEdit}><Pencil className="mr-2 h-4 w-4" /> Modifier les accès</Button>}
            {can(PERMISSIONS.ROLES_DELETE) && <Button variant="outline" onClick={onDelete} className="text-red-600 hover:bg-red-50"><Trash2 className="mr-2 h-4 w-4" /> Supprimer</Button>}
          </div>
        )}
      </div>

      <section className="p-6">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">Ce que ce rôle permet</h3>
        <PermissionMatrix catalogue={catalogue} granted={role.permissions} />
      </section>

      <section className="border-t border-slate-100 p-6">
        <h3 className="mb-3 text-sm font-semibold text-slate-900">
          Personnes avec ce rôle <span className="ml-1 font-normal text-slate-500">({role.userCount})</span>
        </h3>
        {members === null ? (
          <p className="text-sm text-slate-500">Vous n&apos;avez pas la permission de lister les utilisateurs.</p>
        ) : members.length === 0 ? (
          <p className="text-sm text-slate-500">Personne n&apos;a ce rôle pour l&apos;instant.</p>
        ) : (
          <ul className="flex flex-wrap gap-2">
            {members.map((user) => (
              <li key={user.id} className="flex items-center gap-2 rounded-full border border-slate-200 py-1 pl-1 pr-3">
                <Avatar name={displayName(user)} src={user.profilePictureUrl} size="sm" />
                <span className="text-sm text-slate-700">{displayName(user)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </Card>
  );
}
