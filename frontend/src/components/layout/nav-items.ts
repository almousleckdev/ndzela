import { KeyRound, LayoutDashboard, Users, type LucideIcon } from "lucide-react";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  /** When set, the link is only shown to users holding this permission. Omit for pages every signed-in user may open. */
  permission?: Permission;
}

/** Profile and settings live in the header menu, not here. Only pages that exist. Add new pages here, with the permission that guards them. */
export const NAV_ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/", icon: LayoutDashboard },
  { label: "Utilisateurs", href: "/users", icon: Users, permission: PERMISSIONS.USERS_READ },
  { label: "Rôles et permissions", href: "/roles", icon: KeyRound, permission: PERMISSIONS.ROLES_READ },
];
