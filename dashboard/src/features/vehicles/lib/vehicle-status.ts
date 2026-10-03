import type { VehicleStatus } from "@/features/vehicles/types";

export const STATUS_LABELS: Record<VehicleStatus, string> = {
  AVAILABLE: "Disponible",
  ASSIGNED: "Loué",
  IN_MAINTENANCE: "Maintenance",
  ACCIDENT: "Accidenté",
  SUSPENDED: "Suspendu",
  OUT_OF_SERVICE: "Hors service",
};

export const STATUS_VARIANTS = {
  AVAILABLE: "success",
  ASSIGNED: "info",
  IN_MAINTENANCE: "warning",
  ACCIDENT: "danger",
  SUSPENDED: "danger",
  OUT_OF_SERVICE: "danger",
} as const;

// The menu entries that set a status by hand; "assigned" comes only from assigning the car to a driver.
export const STATUS_ACTIONS: { status: Exclude<VehicleStatus, "ASSIGNED">; label: string; title: string }[] = [
  { status: "AVAILABLE", label: "Remettre disponible", title: "Remettre disponible" },
  { status: "IN_MAINTENANCE", label: "Mettre en maintenance", title: "Mettre en maintenance" },
  { status: "ACCIDENT", label: "Déclarer un accident", title: "Déclarer un accident" },
  { status: "SUSPENDED", label: "Suspendre", title: "Suspendre le véhicule" },
  { status: "OUT_OF_SERVICE", label: "Mettre hors service", title: "Mettre hors service" },
];
