"use client";

import { MoreHorizontal } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dropdown, DropdownItem } from "@/components/ui/dropdown";
import { usePermissions } from "@/features/access/hooks/use-permissions";
import { PERMISSIONS } from "@/features/access/lib/permissions";
import type { UserSummary } from "@/features/users/types";

export type UserAction = "edit" | "resend-invitation" | "reset-mfa" | "suspend" | "reactivate" | "block" | "unblock" | "delete";

interface UserActionsMenuProps {
  user: UserSummary;
  isSelf: boolean;
  onAction: (action: UserAction, user: UserSummary) => void;
}

type Item = { action: UserAction; label: string; destructive?: boolean };

// Only offers the actions that make sense for this user's state and that the admin is allowed to do.
export function UserActionsMenu({ user, isSelf, onAction }: UserActionsMenuProps) {
  const { can } = usePermissions();
  const safe: Item[] = [];
  const risky: Item[] = [];

  if (can(PERMISSIONS.USERS_UPDATE)) safe.push({ action: "edit", label: "Modifier" });
  if (can(PERMISSIONS.USERS_CREATE) && user.status === "MFA_SETUP" && !user.mfaEnabled) {
    safe.push({ action: "resend-invitation", label: "Renvoyer l'invitation" });
  }
  if (can(PERMISSIONS.USERS_UPDATE) && user.mfaEnabled && !isSelf) {
    safe.push({ action: "reset-mfa", label: "Réinitialiser l'authentificateur" });
  }
  if (!isSelf) {
    if (can(PERMISSIONS.USERS_LOCK) && user.status === "LOCKED") safe.push({ action: "unblock", label: "Débloquer" });
    if (can(PERMISSIONS.USERS_DEACTIVATE) && user.status === "DISABLED") safe.push({ action: "reactivate", label: "Réactiver" });
    if (can(PERMISSIONS.USERS_DEACTIVATE) && user.status !== "DISABLED") risky.push({ action: "suspend", label: "Suspendre", destructive: true });
    if (can(PERMISSIONS.USERS_LOCK) && user.status !== "LOCKED" && user.status !== "DISABLED") risky.push({ action: "block", label: "Bloquer", destructive: true });
    if (can(PERMISSIONS.USERS_DELETE)) risky.push({ action: "delete", label: "Supprimer", destructive: true });
  }
  if (safe.length + risky.length === 0) return null;

  return (
    <Dropdown trigger={<Button variant="ghost" size="icon" aria-label={`Actions pour ${user.email}`}><MoreHorizontal className="h-4 w-4" /></Button>}>
      {safe.map((item) => <DropdownItem key={item.action} onClick={() => onAction(item.action, user)}>{item.label}</DropdownItem>)}
      {safe.length > 0 && risky.length > 0 && <div className="my-1 border-t border-slate-100" />}
      {risky.map((item) => <DropdownItem key={item.action} destructive onClick={() => onAction(item.action, user)}>{item.label}</DropdownItem>)}
    </Dropdown>
  );
}
