"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { useToast } from "@/hooks/use-toast";
import { useAsync } from "@/hooks/use-async";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { formatDateTime } from "@/lib/time";
import { formatRentDay } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";

interface AllowDaysDialogProps {
  driverId: string;
  driverName: string;
  onClose: () => void;
  onSaved: () => void;
}

// Staff pick the late days a driver with 3+ late days may pay alone in the app; unticking a day takes it back.
export function AllowDaysDialog({ driverId, driverName, onClose, onSaved }: AllowDaysDialogProps) {
  const { addToast } = useToast();
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  const [picked, setPicked] = useState<string[] | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const late = (account.data?.payableDays ?? []).filter((d) => d.late);
  const selected = picked ?? late.filter((d) => d.allowed).map((d) => d.day);
  const toggle = (day: string) => setPicked(late.map((d) => d.day).filter((d) => (d === day ? !selected.includes(d) : selected.includes(d))));

  const save = async (days: string[]) => {
    setIsSaving(true);
    try {
      await paymentsService.allowDays(driverId, days);
      onSaved();
    } catch (error) {
      addToast({ title: "Erreur", description: getErrorMessage(error, "L'action a échoué."), type: "error" });
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Autoriser le paiement partiel" description={`${driverName} paie tout d'un coup à partir de 3 jours en retard. Cochez les jours en retard qu'il peut payer seul dans l'application ; décochez pour retirer l'autorisation.`}>
      {account.isLoading && !account.data ? <p className="text-sm text-slate-600">Chargement du compte…</p> : late.length === 0 ? <p className="text-sm text-slate-600">Aucun jour en retard.</p> : (
        <div className="max-h-72 overflow-y-auto rounded-lg border border-slate-200 p-1">
          {late.map((d) => (
            <Checkbox key={d.day} label={formatRentDay(d.day)} checked={selected.includes(d.day)} onChange={() => toggle(d.day)}
              description={`Échu le ${formatDateTime(d.dueAt)} · ${formatFcfa(d.amount)}${d.penalty > 0 ? ` dont pénalité ${formatFcfa(d.penalty)}` : ""}`} />
          ))}
        </div>
      )}
      <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
        <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
        {selected.length > 0 && <Button variant="outline" onClick={() => save([])} disabled={isSaving}>Tout retirer</Button>}
        <Button onClick={() => save(selected)} isLoading={isSaving} disabled={late.length === 0}>Enregistrer</Button>
      </div>
    </Dialog>
  );
}
