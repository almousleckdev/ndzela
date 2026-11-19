"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Dialog } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { useAsync } from "@/hooks/use-async";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { formatRentDay } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";

interface RescheduleDaysDialogProps {
  driverId: string;
  driverName: string;
  onClose: () => void;
  onSaved: () => void;
}

// Staff push late rent days to the end of the contract (rééchelonner); a pushed day can be put back.
export function RescheduleDaysDialog({ driverId, driverName, onClose, onSaved }: RescheduleDaysDialogProps) {
  const { addToast } = useToast();
  const account = useAsync(() => paymentsService.account(driverId), [driverId]);
  const pushed = useAsync(() => paymentsService.rescheduledDays(driverId), [driverId]);
  const [selected, setSelected] = useState<string[]>([]);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);
  const days = account.data?.payableDays ?? [];
  const toggle = (day: string) => setSelected((current) => (current.includes(day) ? current.filter((d) => d !== day) : [...current, day]));

  const run = async (action: () => Promise<unknown>) => {
    setIsSaving(true);
    try {
      await action();
      onSaved();
    } catch (failure) {
      addToast({ title: "Erreur", description: getErrorMessage(failure, "L'action a échoué."), type: "error" });
      setIsSaving(false);
    }
  };

  const submit = () => {
    if (selected.length === 0) return setError("Choisissez au moins un jour.");
    if (!reason.trim()) return setError("Le motif est obligatoire.");
    return run(() => paymentsService.rescheduleDays(driverId, [...selected].sort(), reason.trim()));
  };

  return (
    <Dialog isOpen onClose={onClose} title="Rééchelonner des jours de loyer" description={`${driverName} ne doit plus ces jours maintenant : ils ne sont plus comptés comme impayés et sont reportés en fin de contrat, qui est prolongé d'autant de jours ouvrés. Un report peut être annulé.`}>
      {account.isLoading && !account.data ? <p className="text-sm text-slate-600">Chargement du compte…</p> : days.length === 0 ? <p className="text-sm text-slate-600">Aucun jour à rééchelonner.</p> : (
        <div className="max-h-60 overflow-y-auto rounded-lg border border-slate-200 p-1">
          {days.map((d) => (
            <Checkbox key={d.day} label={`${formatRentDay(d.day)}${d.late ? " (en retard)" : ""}`} checked={selected.includes(d.day)} onChange={() => toggle(d.day)} description={formatFcfa(d.amount)} />
          ))}
        </div>
      )}
      {selected.length > 0 && <p className="text-xs font-medium text-slate-700">{selected.length} jour{selected.length > 1 ? "s" : ""} sélectionné{selected.length > 1 ? "s" : ""}</p>}
      {days.length > 1 && <button type="button" className="text-xs font-medium text-primary" onClick={() => setSelected(selected.length === days.length ? [] : days.map((d) => d.day))}>Tout sélectionner</button>}
      <div>
        <Label htmlFor="reschedule-reason">Motif *</Label>
        <textarea id="reschedule-reason" rows={2} maxLength={255} value={reason} onChange={(event) => { setReason(event.target.value); setError(undefined); }}
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
        {error && <p role="alert" className="mt-1 text-xs font-medium text-red-600">{error}</p>}
      </div>
      {(pushed.data ?? []).length > 0 && (
        <div className="space-y-1 rounded-lg border border-slate-200 p-2">
          <p className="text-xs font-semibold text-slate-700">Jours déjà reportés</p>
          {(pushed.data ?? []).map((c) => (
            <div key={c.id} className="flex items-center justify-between gap-2 text-sm text-slate-700">
              <span>{formatRentDay(c.originalDay)} → {formatRentDay(c.newDay)} · {c.reason}</span>
              <Button size="sm" variant="outline" disabled={isSaving} onClick={() => run(() => paymentsService.undoReschedule(driverId, c.id))}>Annuler le report</Button>
            </div>
          ))}
        </div>
      )}
      <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
        <Button variant="outline" onClick={onClose} disabled={isSaving}>Fermer</Button>
        <Button onClick={submit} isLoading={isSaving} disabled={days.length === 0}>Rééchelonner</Button>
      </div>
    </Dialog>
  );
}
