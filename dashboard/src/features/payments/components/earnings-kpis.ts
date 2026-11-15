import type { Kpi } from "@/components/layout/page";
import { formatFcfa } from "@/lib/money";
import type { Earnings } from "@/features/payments/types";

// The five figures at the top of "Total encaissé"; dashes while loading.
export function earningsKpis(earnings: Earnings | null): Kpi[] {
  const fcfa = (value: number | undefined) => (earnings && value !== undefined ? formatFcfa(value) : "-");
  return [
    { label: "Encaissé", value: fcfa(earnings?.collected), hint: "Loyers payés, sans les paiements annulés" },
    { label: "Frais opérateur", value: fcfa(earnings?.fees) },
    { label: "Net", value: fcfa(earnings?.net), hint: "Encaissé moins les frais" },
    { label: "Chauffeurs actuels", value: fcfa(earnings?.collectedCurrent), hint: earnings ? `${earnings.payingDrivers} chauffeur(s) ont payé` : undefined },
    { label: "Anciens chauffeurs", value: fcfa(earnings?.collectedFormer), hint: "Profils archivés" },
  ];
}
