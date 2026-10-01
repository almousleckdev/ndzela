import type { ArrearsLevel, DayStatus, EntryType, PaymentMethod } from "@/features/payments/types";

export const METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Espèces",
  AIRTEL_MONEY: "Airtel Money",
  MOOV_MONEY: "Moov Money",
  OTHER: "Autre",
};

export const ENTRY_TYPE_LABELS: Record<EntryType, string> = {
  PAYMENT: "Paiement",
  REVERSAL: "Annulation",
  WAIVER: "Délai accordé",
  PENALTY: "Pénalité",
};

export const LEVEL_LABELS: Record<ArrearsLevel, string> = {
  NONE: "À jour",
  LATE: "En retard",
  YELLOW: "Retard 24 h",
  DARK_YELLOW: "Retard 48 h",
  RED: "Retard 72 h",
};

/** Badge classes per level: neutral, yellow, dark yellow, red. */
export const LEVEL_BADGE: Record<ArrearsLevel, string> = {
  NONE: "bg-emerald-50 text-emerald-700",
  LATE: "bg-slate-100 text-slate-700",
  YELLOW: "bg-yellow-100 text-yellow-800",
  DARK_YELLOW: "bg-amber-300 text-amber-950",
  RED: "bg-red-100 text-red-700",
};

/** Row tint per level, so the table reads at a glance. */
export const LEVEL_ROW: Record<ArrearsLevel, string> = {
  NONE: "",
  LATE: "",
  YELLOW: "bg-yellow-50",
  DARK_YELLOW: "bg-amber-100/70",
  RED: "bg-red-50",
};

export const DAY_STATUS_LABELS: Record<DayStatus, string> = {
  PAID_ON_TIME: "Payé à temps",
  PAID_LATE: "Payé en retard",
  UNPAID: "Impayé",
};

export const DAY_STATUS_CLASS: Record<DayStatus, string> = {
  PAID_ON_TIME: "bg-emerald-50 text-emerald-700",
  PAID_LATE: "bg-amber-100 text-amber-800",
  UNPAID: "bg-red-100 text-red-700",
};
