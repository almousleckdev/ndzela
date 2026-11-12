"use client";

import { useState } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAsync } from "@/hooks/use-async";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { IncidentDialog } from "@/features/payments/components/incident-dialog";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { IncidentCause } from "@/features/payments/types";

const CAUSES: Record<IncidentCause, string> = { ELECTRICITY: "Coupure d'électricité", LATE_SIGNING: "Contrat signé après 12 h", OTHER: "Autre" };
const day = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("fr-FR", { weekday: "short", day: "numeric", month: "short", year: "numeric" });

// The driver's half days and stops, with the means to declare and cancel them (tab of "Gérer les arrêts").
export function HalfDaysPanel({ driverId, driverName, contractStart, canDeclare, onChanged }: { driverId: string; driverName: string; contractStart?: string; canDeclare: boolean; onChanged: () => void }) {
  const { addToast } = useToast();
  const list = useAsync(() => paymentsService.incidents(driverId), [driverId]);
  const [declaring, setDeclaring] = useState(false);

  const cancel = async (id: string) => {
    try {
      await paymentsService.cancelIncident(driverId, id);
      addToast({ title: "Succès", description: "Déclaration annulée : le loyer complet est dû ce jour.", type: "success" });
      list.reload();
      onChanged();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "Impossible d'annuler."), type: "error" });
    }
  };

  return (
    <div className="space-y-4">
      {canDeclare && (
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm text-slate-600">Le chauffeur ne paie que la moitié du loyer ce jour-là.</p>
          <Button onClick={() => setDeclaring(true)}>Déclarer une demi-journée</Button>
        </div>
      )}
      {!list.data ? <Skeleton className="h-16 w-full" /> : list.data.length === 0 ? (
        <p className="text-sm text-slate-500">Aucune demi-journée ni aucun arrêt de loyer déclaré.</p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {list.data.map((item) => (
            <li key={item.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2.5 text-sm">
              <div>
                <p className="font-medium text-slate-900">{day(item.day)} · {item.kind === "HALF_DAY" ? "Demi-journée" : "Arrêt"}</p>
                <p className="text-xs text-slate-500">{CAUSES[item.cause]}{item.hours ? ` (${item.hours} h)` : ""}{item.note ? ` · ${item.note}` : ""} · par {item.createdBy}</p>
              </div>
              <div className="flex items-center gap-2">
                {item.cancelledAt ? <Badge variant="neutral">Annulée</Badge> : <Badge variant="info">Active</Badge>}
                {canDeclare && !item.cancelledAt && <Button variant="ghost" size="sm" className="text-red-600 hover:bg-red-50" onClick={() => cancel(item.id)}>Annuler</Button>}
              </div>
            </li>
          ))}
        </ul>
      )}
      {declaring && <IncidentDialog driverId={driverId} driverName={driverName} kind="HALF_DAY" contractStart={contractStart} onClose={() => setDeclaring(false)} onSaved={() => { setDeclaring(false); list.reload(); onChanged(); }} />}
    </div>
  );
}
