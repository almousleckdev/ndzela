"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { paymentsService } from "@/features/payments/services/payments.service";

interface WaiverDialogProps {
  driverId: string;
  driverName: string;
  overdueAmount: number;
  dailyRent: number | null;
  onClose: () => void;
  onSaved: () => void;
}

/** An administrator forgives part of the rent owed (a delay): it settles the oldest days like a payment and is kept with its reason. */
export function WaiverDialog({ driverId, driverName, overdueAmount, dailyRent, onClose, onSaved }: WaiverDialogProps) {
  const { addToast } = useToast();
  const [amount, setAmount] = useState(dailyRent ? String(dailyRent) : "");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  const save = async () => {
    if (!/^\d{1,14}$/.test(amount) || Number(amount) < 1) return setError("Saisissez un montant entier supérieur à 0.");
    if (!reason.trim()) return setError("Le motif est obligatoire.");
    setIsSaving(true);
    try {
      await paymentsService.waive(driverId, Number(amount), reason.trim());
      addToast({ title: "Succès", description: "Délai accordé.", type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible d'accorder le délai."));
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Accorder un délai" description={`${driverName}. Dû actuellement : ${formatFcfa(overdueAmount)}.`}>
      <div className="space-y-4">
        <FormField label="Montant pardonné (FCFA) *" inputMode="numeric" value={amount} onChange={(event) => { setAmount(event.target.value); setError(undefined); }}
          hint="Un jour de loyer à la fois est le plus courant. Cette écriture est conservée avec son motif." />
        <div>
          <Label htmlFor="waiver-reason">Motif *</Label>
          <textarea id="waiver-reason" rows={3} maxLength={255} value={reason} onChange={(event) => { setReason(event.target.value); setError(undefined); }}
            className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
          <Button onClick={save} isLoading={isSaving}>Accorder le délai</Button>
        </div>
      </div>
    </Dialog>
  );
}
