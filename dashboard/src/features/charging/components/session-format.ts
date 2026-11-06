import type { ChargingSessionRow } from "@/features/charging/types";

const STATUS: Record<string, string> = { COMPLETED: "Terminée", CHARGING: "En charge", PREPARING: "Préparation", FINISHING: "Fin de charge", FAULTED: "En panne" };
const DRIVER_STOPPED = "Arrêt par le conducteur";
const AMOUNT_REACHED = "Montant atteint";
const POWER_CUT = "Coupure d'électricité";
const REASON: Record<string, string> = {
  Remote: DRIVER_STOPPED, Local: DRIVER_STOPPED, EVDisconnected: DRIVER_STOPPED, EmergencyStop: DRIVER_STOPPED, DeAuthorized: DRIVER_STOPPED, UnlockCommand: DRIVER_STOPPED,
  Other: AMOUNT_REACHED, PowerLoss: POWER_CUT, Reboot: POWER_CUT, HardReset: POWER_CUT, SoftReset: POWER_CUT,
};

export const statusLabel = (status: string | null) => (status ? STATUS[status] ?? status : null);
// Why a session stopped, in three words: the driver stopped it, the amount was reached, or the power was cut. A finished session is never left without one.
export const reasonLabel = (reason: string | null, finished = true) => (reason && REASON[reason]) || (finished ? AMOUNT_REACHED : null);
export const isRunning = (row: ChargingSessionRow) => row.startedAt !== null && row.endedAt === null;

// A session still under way is measured up to now.
export function durationLabel(row: ChargingSessionRow): string | null {
  if (!row.startedAt) return null;
  const end = row.endedAt ? new Date(row.endedAt).getTime() : Date.now();
  const minutes = Math.max(0, Math.round((end - new Date(row.startedAt).getTime()) / 60000));
  return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, "0")}`;
}

// The live power while charging, the average power once finished
export function powerLabel(row: ChargingSessionRow): string | null {
  if (isRunning(row)) return row.powerKw !== null ? `${row.powerKw} kW en ce moment` : null;
  if (row.energyKwh === null || !row.durationSeconds) return null;
  return `moy. ${(row.energyKwh / (row.durationSeconds / 3600)).toFixed(1)} kW`;
}

export function batteryLabel(row: ChargingSessionRow): { primary: string; secondary?: string } | null {
  const { socBefore: before, socAfter: after } = row;
  if (after === null) return before === null ? null : { primary: `${before} %` };
  if (before === null) return { primary: `${after} %`, secondary: isRunning(row) ? "niveau actuel" : "niveau final" };
  return { primary: `${before} % → ${after} %`, secondary: after > before ? `+${after - before} %` : undefined };
}

export const PAYMENT_STATE: Record<string, { label: string; variant: "success" | "warning" | "danger" | "info" | "neutral" }> = {
  CHARGED: { label: "Payé", variant: "success" },
  PENDING: { label: "En attente", variant: "info" },
  NOT_CHARGED: { label: "Non débité", variant: "danger" },
  UNDER_REVIEW: { label: "En vérification", variant: "warning" },
  REFUNDED: { label: "Remboursé", variant: "neutral" },
};
