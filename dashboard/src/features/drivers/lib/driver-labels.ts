import type { Civility, Driver, DriverStatus, LeaveReason, IdType, LicenceCategory, Relationship } from "@/features/drivers/types";

export const CIVILITY_LABELS: Record<Civility, string> = { MALE: "Homme", FEMALE: "Femme" };

export const ID_TYPE_LABELS: Record<IdType, string> = { NATIONAL_ID: "Carte nationale d'identité", PASSPORT: "Passeport" };

export const LICENCE_CATEGORIES: LicenceCategory[] = ["A", "B", "C", "D"];

export const RELATIONSHIP_LABELS: Record<Relationship, string> = {
  FATHER: "Père",
  MOTHER: "Mère",
  BROTHER: "Frère",
  SPOUSE: "Époux/Épouse",
  SISTER: "Sœur",
  UNCLE: "Oncle",
  AUNT: "Tante",
  COUSIN: "Cousin(e)",
  FRIEND: "Ami(e)",
  OTHER: "Autre",
};

export const DRIVER_STATUS_LABELS: Record<DriverStatus, string> = { ACTIVE: "Actif", SUSPENDED: "Suspendu", BLOCKED: "Bloqué", INACTIVE: "Inactif" };

export const DRIVER_STATUS_VARIANTS = { ACTIVE: "success", SUSPENDED: "warning", BLOCKED: "danger", INACTIVE: "neutral" } as const;

// The badge of a driver
export function displayStatus(driver: Driver): { label: string; variant: "success" | "warning" | "danger" | "neutral" | "info" } {
  if (driver.status === "ACTIVE" && driver.onLeave) return { label: "En arrêt", variant: "info" };
  return { label: DRIVER_STATUS_LABELS[driver.status], variant: DRIVER_STATUS_VARIANTS[driver.status] };
}

export const LEAVE_REASON_LABELS: Record<LeaveReason, string> = {
  VEHICLE_BREAKDOWN: "Panne véhicule",
  ACCIDENT: "Accident",
  MAINTENANCE: "Entretien / révision",
  PUBLIC_HOLIDAY: "Jour férié",
  WEATHER: "Intempéries",
  ADMINISTRATIVE: "Problème administratif",
  PERSONAL: "Convenance personnelle",
  OTHER: "Autre",
};

export const toOptions = <T extends string>(labels: Record<T, string>) =>
  (Object.entries(labels) as [T, string][]).map(([value, label]) => ({ value, label }));
