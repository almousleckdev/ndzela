import { BatteryCharging, Car, Contact, Inbox, FileClock, History, KeyRound, LayoutDashboard, TriangleAlert, Users, Wallet, type LucideIcon } from "lucide-react";
import { PERMISSIONS, type Permission } from "@/features/access/lib/permissions";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  // When set, the link is only shown to users holding this permission.
  permission?: Permission;
  // A live counter shown at the end of the row (from the backend).
  badge?: "arrears" | "inquiries";
}

export interface NavSection {
  label: string;
  items: NavItem[];
}

// Profile and settings live in the header menu, not here.
export const NAV_SECTIONS: NavSection[] = [
  { label: "Pilotage", items: [{ label: "Tableau de bord", href: "/", icon: LayoutDashboard }] },
  {
    label: "Finance",
    items: [
      { label: "Suivi paiements", href: "/finance/payments", icon: Wallet, permission: PERMISSIONS.PAYMENTS_READ },
      { label: "Gérer impayés", href: "/finance/arrears", icon: TriangleAlert, permission: PERMISSIONS.PAYMENTS_READ, badge: "arrears" },
      { label: "Historique", href: "/finance/history", icon: FileClock, permission: PERMISSIONS.PAYMENTS_READ },
    ],
  },
  { label: "Énergie", items: [{ label: "Recharges", href: "/charging", icon: BatteryCharging, permission: PERMISSIONS.CHARGING_READ }] },
  {
    label: "Flotte",
    items: [
      { label: "Chauffeurs", href: "/drivers", icon: Contact, permission: PERMISSIONS.DRIVERS_READ },
      { label: "Véhicules", href: "/vehicles", icon: Car, permission: PERMISSIONS.VEHICLES_READ },
      { label: "Candidatures", href: "/inquiries", icon: Inbox, permission: PERMISSIONS.INQUIRIES_READ, badge: "inquiries" },
    ],
  },
  {
    label: "Administration",
    items: [
      { label: "Utilisateurs", href: "/users", icon: Users, permission: PERMISSIONS.USERS_READ },
      { label: "Rôles et permissions", href: "/roles", icon: KeyRound, permission: PERMISSIONS.ROLES_READ },
      { label: "Journal d'audit", href: "/audit", icon: History, permission: PERMISSIONS.AUDIT_READ },
    ],
  },
];
