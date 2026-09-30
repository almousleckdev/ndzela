import { PermissionMatrix } from "@/features/access/components/permission-matrix";
import { Card } from "@/components/ui/card";
import { ALL_PERMISSIONS, roleDescription } from "@/features/access/lib/permissions";
import type { UserProfile } from "@/features/auth/types";

/** "What can I do?" in one grid, followed by what each of my roles is for. */
export function MyAccessCard({ user }: { user: UserProfile }) {
  return (
    <Card className="lg:p-8">
      <h3 className="font-display text-xl font-semibold text-slate-900">Mes accès</h3>
      <p className="mb-5 mt-1 text-sm text-slate-500">Ce que vous pouvez faire dans NDZela Admin. Une case orange signifie « autorisé ».</p>

      <PermissionMatrix catalogue={ALL_PERMISSIONS} granted={user.permissions} />

      <ul className="mt-6 space-y-2 border-t border-slate-100 pt-5">
        {user.roles.map((role) => (
          <li key={role} className="text-sm text-slate-600"><span className="font-medium text-slate-900">{role.replaceAll("_", " ")}</span>: {roleDescription(role)}</li>
        ))}
      </ul>
    </Card>
  );
}
