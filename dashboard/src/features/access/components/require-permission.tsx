"use client";

import { ShieldAlert } from "lucide-react";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import type { Permission } from "@/features/access/lib/permissions";
import { EmptyState } from "@/components/ui/empty-state";

// Wrap a page
export function RequirePermission({ permission, children }: { permission: Permission; children: React.ReactNode }) {
  const { can } = usePermissions();
  if (can(permission)) return <>{children}</>;
  return (
    <div className="mx-auto max-w-lg pt-16 text-center">
      <ShieldAlert className="mx-auto mb-4 h-10 w-10 text-slate-400" />
      <EmptyState title="Accès refusé" description="Votre rôle ne vous permet pas de consulter cette page. Contactez un super administrateur." />
    </div>
  );
}
