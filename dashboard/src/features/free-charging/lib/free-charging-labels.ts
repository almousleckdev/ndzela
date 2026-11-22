import type { AgreementStatus, EligibilityReason } from "@/features/free-charging/types";

export const ELIGIBILITY_LABELS: Record<EligibilityReason, string> = {
  ELIGIBLE: "Éligible",
  NO_AGREEMENT: "Aucun accord",
  AGREEMENT_SUSPENDED: "Accord suspendu",
  AGREEMENT_NOT_STARTED: "Accord pas encore commencé",
  AGREEMENT_ENDED: "Accord terminé",
  OUTSIDE_WINDOW: "Hors créneau",
  UNPAID_DAYS: "Jours impayés",
  DRIVER_SUSPENDED: "Chauffeur suspendu",
  NOT_LINKED: "Compte Zella non lié",
};

export const STATUS_LABELS: Record<AgreementStatus, string> = {
  ACTIVE: "Actif",
  SUSPENDED: "Suspendu",
  REVOKED: "Révoqué",
};

export const STATUS_VARIANTS: Record<AgreementStatus, "success" | "warning" | "danger"> = {
  ACTIVE: "success",
  SUSPENDED: "warning",
  REVOKED: "danger",
};

export function eligibilityLabel(reason: EligibilityReason): string {
  return ELIGIBILITY_LABELS[reason] ?? reason;
}

export function formatDay(day: string | null): string {
  if (!day) return "";
  const [year, month, date] = day.split("-");
  return `${date}/${month}/${year}`;
}

