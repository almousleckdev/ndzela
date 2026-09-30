import { roleLabel } from "@/features/access/lib/permissions";
import { roleTone } from "@/features/access/lib/role-style";
import { cn } from "@/lib/utils";

export function RoleChip({ role, className }: { role: string; className?: string }) {
  const tone = roleTone(role);
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset", tone.chip, className)}>
      <span className={cn("h-1.5 w-1.5 rounded-full", tone.dot)} />
      {roleLabel(role)}
    </span>
  );
}
