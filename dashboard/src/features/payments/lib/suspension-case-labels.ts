import type { SuspensionCase } from "@/features/payments/types";

const REASONS: Record<string, string> = {
  OVERLAP: "Deux véhicules chargés en même temps",
  BATTERY_CHAIN: "Batterie incohérente entre deux recharges",
  ENERGY: "Énergie très supérieure à l'habitude",
};

const REFERENCE_LENGTH = 8;

export function reasonLabel(code: string): string {
  return REASONS[code] ?? code;
}

// The other account as staff may know it: a driver of ours, or only the short reference Zella gave
export function helperLabel(row: Pick<SuspensionCase, "helper" | "helperReference">): { primary: string; secondary?: string } {
  if (row.helper) {
    return { primary: row.helper.name, secondary: row.helper.vehicle ?? undefined };
  }
  return { primary: "Compte Zella", secondary: `réf. ${row.helperReference.slice(0, REFERENCE_LENGTH)}` };
}

export function scoreLabel(row: Pick<SuspensionCase, "score" | "flagged" | "reasons">): string {
  if (row.score === null) {
    return "Non évalué";
  }
  return row.flagged ? row.reasons.map(reasonLabel).join(" · ") : "Rien d'anormal";
}
