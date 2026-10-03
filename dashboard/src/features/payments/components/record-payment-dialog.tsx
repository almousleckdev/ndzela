"use client";

import { useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog } from "@/components/ui/dialog";
import { FormField } from "@/components/ui/form-field";
import { SelectField } from "@/components/ui/select-field";
import { useToast } from "@/hooks/use-toast";
import { getErrorMessage } from "@/lib/errors";
import { formatFcfa } from "@/lib/money";
import { useRentMethods } from "@/features/payments/hooks/use-rent-methods";
import { LEVEL_LABELS } from "@/features/payments/lib/payment-labels";
import { paymentsService } from "@/features/payments/services/payments.service";
import type { PaymentMethod } from "@/features/payments/types";

interface RecordPaymentDialogProps {
  driverId: string;
  driverName: string;
  // What the driver owes now, shown as a reminder.
  overdueAmount: number;
  dailyRent: number | null;
  onClose: () => void;
  onSaved: () => void;
}

// Records a payment received from a driver.
export function RecordPaymentDialog({ driverId, driverName, overdueAmount, dailyRent, onClose, onSaved }: RecordPaymentDialogProps) {
  const { addToast } = useToast();
  const requestId = useRef(crypto.randomUUID());
  const [amount, setAmount] = useState(overdueAmount > 0 ? String(overdueAmount) : dailyRent ? String(dailyRent) : "");
  const methods = useRentMethods();
  const [chosen, setChosen] = useState<PaymentMethod | "">("");
  const method = (chosen || methods[0]?.value || "") as PaymentMethod;
  const [reference, setReference] = useState("");
  const [error, setError] = useState<string>();
  const [isSaving, setIsSaving] = useState(false);

  const save = async () => {
    if (!/^\d{1,14}$/.test(amount) || Number(amount) < 1) return setError("Saisissez un montant entier supérieur à 0.");
    if (!method) return setError("Aucun moyen de paiement disponible pour ce pays.");
    if (!reference.trim()) return setError("Le numéro de la transaction est obligatoire.");
    setIsSaving(true);
    try {
      const result = await paymentsService.record({ driverId, amount: Number(amount), method, reference: reference.trim(), requestId: requestId.current });
      const rest = result.overdueAmount > 0 ? `Reste dû : ${formatFcfa(result.overdueAmount)} (${LEVEL_LABELS[result.level]}).` : result.credit > 0 ? `Crédit : ${formatFcfa(result.credit)}.` : "Le chauffeur est à jour.";
      addToast({ title: "Paiement enregistré", description: `${formatFcfa(Number(amount))}. ${rest}`, type: "success" });
      onSaved();
    } catch (failure) {
      setError(getErrorMessage(failure, "Impossible d'enregistrer le paiement."));
      setIsSaving(false);
    }
  };

  return (
    <Dialog isOpen onClose={onClose} title="Enregistrer un paiement" description={driverName}>
      <div className="space-y-4">
        {overdueAmount > 0 && <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">Montant dû actuellement : <strong>{formatFcfa(overdueAmount)}</strong></p>}
        <FormField label="Montant reçu (FCFA) *" inputMode="numeric" value={amount} onChange={(event) => { setAmount(event.target.value); setError(undefined); }}
          hint="Les paiements règlent d'abord les jours les plus anciens, jour entier par jour entier." />
        <SelectField label="Moyen de paiement *" options={methods} value={method}
          onChange={(event) => { setChosen(event.target.value as PaymentMethod); setError(undefined); }} />
        <FormField label="Numéro de la transaction *" value={reference} onChange={(event) => { setReference(event.target.value); setError(undefined); }}
          maxLength={100} hint="Le numéro reçu par SMS de l'opérateur : il permet de retrouver le paiement." />
        {error && <p role="alert" className="text-xs font-medium text-red-600">{error}</p>}
        <div className="flex justify-end gap-3 border-t border-slate-200 pt-4">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>Annuler</Button>
          <Button onClick={save} isLoading={isSaving}>Enregistrer</Button>
        </div>
      </div>
    </Dialog>
  );
}
