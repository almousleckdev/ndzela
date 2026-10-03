import type { UserStatus } from "@/features/users/types";

export const STATUS_META: Record<UserStatus, { label: string; dot: string; text: string }> = {
  ACTIVE: { label: "Actif", dot: "bg-emerald-500", text: "text-emerald-800" },
  MFA_SETUP: { label: "Invitation en attente", dot: "bg-amber-500", text: "text-amber-800" },
  LOCKED: { label: "Verrouillé", dot: "bg-rose-500", text: "text-rose-800" },
  DISABLED: { label: "Désactivé", dot: "bg-slate-400", text: "text-slate-600" },
};
